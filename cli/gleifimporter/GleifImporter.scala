//> using jvm 25
//> using dep org.postgresql:postgresql:42.7.10
//> using dep org.flywaydb:flyway-core:12.0.2
//> using dep org.flywaydb:flyway-database-postgresql:12.0.2
//> using dep com.lihaoyi::os-lib:0.11.9-M6
//> using dep com.softwaremill.sttp.client4::core:4.0.19
//> using dep com.softwaremill.ox::core:1.0.4
//> using dep com.zaxxer:HikariCP:7.0.2
//> using dep com.augustnagro::magnum:1.3.1
//> using file Database.scala

import java.io.FileInputStream
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants as C}
import org.postgresql.ds.PGSimpleDataSource
import os.Path
import ox.*
import ox.flow.Flow
import sttp.client4.*
import sttp.client4.httpclient.HttpClientSyncBackend

// ── XML event model ──────────────────────────────────────────────────────────
enum XmlEvent:
  case Open(name: String, attrs: Map[String, String])
  case Close(name: String)
  case Text(value: String)

case class Config(
    dataDir: String = "./gleif_data",
    dbUrl: String = "jdbc:postgresql://localhost:5434/gleif",
    dbUser: String = "esap_user",
    dbPassword: String = "esap_password"
)

// ── Main application ─────────────────────────────────────────────────────────
object GleifImporter extends OxApp:

  enum GleifFileType(val name: String, val url: String):
    case Lei extends GleifFileType(
          "lei-cdf-concatenated",
          "https://leidata.gleif.org/api/v1/concatenated-files/lei2/latest/zip"
        )
    case Rr  extends GleifFileType(
          "rr-cdf-concatenated",
          "https://leidata.gleif.org/api/v1/concatenated-files/rr/latest/zip"
        )

  def run(args: Vector[String])(using Ox): ExitCode =
    val config  = parseArgs(args)
    val dataDir = os.Path(config.dataDir, os.pwd)
    if !os.exists(dataDir) then os.makeDir(dataDir)

    println("=" * 60)
    println("GLEIF Data Importer")
    println("=" * 60)

    println("\n[1/3] Setting up database with Flyway...")
    Database.setupDatabase(config.dbUrl, config.dbUser, config.dbPassword, os.pwd / "migrations")

    println("\n[2/3] Downloading and importing GLEIF data (parallel)...")
    par(
      processFile(GleifFileType.Lei, config),
      processFile(GleifFileType.Rr, config)
    )

    println("\n[3/3] Verifying import...")
    Database.verifyImport(config.dbUrl, config.dbUser, config.dbPassword)

    println("\n" + "=" * 60)
    println("Import complete!")
    println("=" * 60)
    ExitCode.Success

  // ── Config parsing ──────────────────────────────────────────────────────────
  def parseArgs(args: Vector[String]): Config =
    args
      .sliding(2, 2)
      .foldLeft(Config()):
        case (cfg, Vector("--db-url", v))      => cfg.copy(dbUrl = v)
        case (cfg, Vector("--db-user", v))     => cfg.copy(dbUser = v)
        case (cfg, Vector("--db-password", v)) => cfg.copy(dbPassword = v)
        case (cfg, Vector("--data-dir", v))    => cfg.copy(dataDir = v)
        case (cfg, _)                          => cfg

  // ── File handling ───────────────────────────────────────────────────────────
  def processFile(fileType: GleifFileType, config: Config): Unit =
    val tag     = s"[${fileType.name}]"
    val xmlFile = resolveXmlFile(fileType, config)
    val sizeMb  = os.size(xmlFile) / 1024 / 1024

    println(s"  $tag Parsing XML and inserting ($sizeMb MB)...")

    xmlFlow(xmlFile)
      .mapStateful(RecordState()): (state, event) =>
        val next = event match
          case XmlEvent.Open("LEIRecord", _)                                       => RecordState(active = true)
          case XmlEvent.Close("LEIRecord")                                         => state.copy(active = false)
          case XmlEvent.Open(name, attrs) if state.active                          =>
            val lang = if name == "LegalName" then attrs.get("lang") else None
            state.copy(
              currentField = name,
              fields = lang.fold(state.fields)(l => state.fields + ("LegalName@lang" -> l))
            )
          case XmlEvent.Text(value) if state.active && state.currentField.nonEmpty =>
            state.copy(fields = state.fields + (state.currentField -> value))
          case _                                                                   => state

        val maybeRecord =
          if event == XmlEvent.Close("LEIRecord") && state.active && state.fields.contains("LEI") then
            Some(Database.LeiRecord(
              id = state.fields("LEI"),
              legalName = state.fields.getOrElse("LegalName", ""),
              legalNameLanguage = state.fields.getOrElse("LegalName@lang", ""),
              initialRegistrationDate = state.fields.getOrElse("InitialRegistrationDate", ""),
              lastUpdateDate = state.fields.getOrElse("LastUpdateDate", ""),
              status = state.fields.getOrElse("RegistrationStatus", ""),
              nextRenewalDate = state.fields.getOrElse("NextRenewalDate", "")
            ))
          else None

        (next, maybeRecord)
      .collect { case Some(r) => r }
      .grouped(50_000)
      .mapPar(4): chunk =>
        Database.insertChunk(chunk, config.dbUrl, config.dbUser, config.dbPassword)
      .runDrain()

    println(s"  $tag Done.")

  def resolveXmlFile(fileType: GleifFileType, config: Config): Path =
    val dataDir = os.Path(config.dataDir, os.pwd)
    val zipFile = dataDir / s"${fileType.name}.zip"
    val xmlFile = dataDir / s"${fileType.name}.xml"

    if os.exists(xmlFile) then
      println(s"  [${fileType.name}] Using cached XML: ${xmlFile.last}")
      xmlFile
    else if os.exists(zipFile) then
      println(s"  [${fileType.name}] Using cached ZIP, extracting...")
      extractZip(zipFile, xmlFile)
      // os.remove(zipFile)
      xmlFile
    else
      println(s"  [${fileType.name}] Downloading from ${fileType.url}...")
      downloadFile(fileType.url, zipFile)
      println(s"  [${fileType.name}] Extracting ZIP...")
      extractZip(zipFile, xmlFile)
      // os.remove(zipFile)
      xmlFile

  def downloadFile(url: String, target: Path): Unit =
    val backend = HttpClientSyncBackend()
    try
      basicRequest
        .header("User-Agent", "GLEIF-Importer/1.0")
        .get(uri"$url")
        .response(asByteArray)
        .send(backend)
        .body match
        case Right(bytes) =>
          os.write(target, bytes)
          println(s"    Downloaded ${bytes.length / 1024 / 1024} MB to ${target.last}")
        case Left(err)    =>
          throw RuntimeException(s"HTTP ${backend} failed: $err")
    finally backend.close()

  def extractZip(zipPath: Path, targetXml: Path): Unit =
    val tempDir = zipPath / os.up / "temp_extract"
    os.makeDir.all(tempDir)
    os.unzip(zipPath, tempDir)
    os.list(tempDir).find(_.last.endsWith(".xml")) match
      case Some(xml) => os.move.over(xml, targetXml)
      case None      => println("    WARNING: No XML file found in ZIP")
    os.remove.all(tempDir)

  // ── XML streaming ───────────────────────────────────────────────────────────

  /** Wraps a StAX reader into a lazy [[Flow]] of [[XmlEvent]]s. The file and reader are closed automatically after the flow is run.
    */
  def xmlFlow(file: Path): Flow[XmlEvent] =
    // Lift JDK XML entity size limits – GLEIF files contain large text nodes
    System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0")
    System.setProperty("jdk.xml.totalEntitySizeLimit", "0")

    val factory = XMLInputFactory.newInstance().tap: f =>
      f.setProperty(XMLInputFactory.IS_COALESCING, true)
      f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
      f.setProperty(XMLInputFactory.SUPPORT_DTD, false)

    Flow.usingEmit: emit =>
      val stream = FileInputStream(file.toString)
      val reader = factory.createXMLStreamReader(stream)
      try
        while reader.hasNext do
          reader.next match
            case C.START_ELEMENT =>
              val attrs = (0 until reader.getAttributeCount)
                .map(i => reader.getAttributeLocalName(i) -> reader.getAttributeValue(i))
                .toMap
              emit(XmlEvent.Open(reader.getLocalName, attrs))
            case C.END_ELEMENT   =>
              emit(XmlEvent.Close(reader.getLocalName))
            case C.CHARACTERS    =>
              val text = reader.getText.trim
              if text.nonEmpty then emit(XmlEvent.Text(text))
            case _               => ()
      finally
        reader.close()
        stream.close()

  private case class RecordState(
      active: Boolean = false,
      currentField: String = "",
      fields: Map[String, String] = Map.empty
  )

end GleifImporter
