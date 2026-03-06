//> using jvm 25
//> using dep org.postgresql:postgresql:42.7.10
//> using dep org.flywaydb:flyway-core:12.0.3
//> using dep org.flywaydb:flyway-database-postgresql:12.0.3
//> using dep com.lihaoyi::os-lib:0.11.9-M6
//> using dep com.softwaremill.sttp.client4::core:4.0.19
//> using dep com.softwaremill.ox::core:1.0.4
//> using dep com.zaxxer:HikariCP:7.0.2
//> using dep com.augustnagro::magnum:1.3.1
//> using dep com.fasterxml:aalto-xml:1.3.4
//> using file Database.scala

import com.fasterxml.aalto.stax.InputFactoryImpl
import os.Path
import ox.*
import ox.flow.Flow
import sttp.client4.*
import sttp.client4.httpclient.HttpClientSyncBackend

import java.io.FileInputStream
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.stream.XMLStreamConstants as C

// ── Main application ─────────────────────────────────────────────────────────
object GleifImporter extends OxApp:

  def run(args: Vector[String])(using Ox): ExitCode =
    val config                       = Config()
    val dataDir                      = os.Path(config.dataDir, os.pwd)
    if !os.exists(dataDir) then os.makeDir(dataDir)
    val gleifFileType: GleifFileType = GleifFileType.GoldenCopyDelta(DeltaType.IntraDay)

    println("=" * 60)
    println(s"GLEIF Data Importer for $gleifFileType")
    println("=" * 60)

    println("\n[1/3] Setting up database with Flyway...")
    Database.setupDatabase(config.dbUrl, config.dbUser, config.dbPassword, os.pwd / "migrations")

    downloadXsd(dataDir, gleifFileType)

    println("\n[2/3] Downloading and importing GLEIF data...")
    processFile(gleifFileType, config)

    println("\n[3/3] Verifying import...")
    Database.verifyImport(config.dbUrl, config.dbUser, config.dbPassword)

    println(s"\n${"=" * 60}")
    println("Import complete!")
    println("=" * 60)
    ExitCode.Success

  // ── File handling ───────────────────────────────────────────────────────────
  def processFile(fileType: GleifFileType, config: Config): Unit =
    val tag     = s"[${fileType.name}]"
    val xmlFile = resolveXmlFile(fileType, config)
    val sizeMb  = os.size(xmlFile) / 1024 / 1024

    println(s"  $tag Parsing XML and inserting ($sizeMb MB)...")

    val totalInserted = AtomicInteger(0)

    xmlFlow(xmlFile)
      .mapStateful(RecordState()): (state, event) =>
        val next = event match
          case XmlEvent.Open(LeiTags.LEIRecord, _)                                 =>
            RecordState(active = true)
          case XmlEvent.Close(LeiTags.LEIRecord)                                   =>
            state.copy(active = false)
          case XmlEvent.Open(name, attrs) if state.active                          =>
            val lang = if name == LeiTags.LegalName then attrs.get("lang") else None
            state.copy(
              currentField = name,
              fields = lang.fold(state.fields)(l => state.fields + (LeiTags.LegalNameLang -> l))
            )
          case XmlEvent.Text(value) if state.active && state.currentField.nonEmpty =>
            state.copy(fields = state.fields + (state.currentField -> value))
          case _                                                                   => state

        val maybeRecord =
          if event == XmlEvent.Close(LeiTags.LEIRecord) && state.active && state.fields.contains(LeiTags.LEI) then
            Some(Database.LeiRecord(
              id = state.fields(LeiTags.LEI),
              legalName = state.fields.getOrElse(LeiTags.LegalName, ""),
              legalNameLanguage = state.fields.getOrElse(LeiTags.LegalNameLang, ""),
              initialRegistrationDate = state.fields.getOrElse(LeiTags.InitialRegistrationDate, ""),
              lastUpdateDate = state.fields.getOrElse(LeiTags.LastUpdateDate, ""),
              status = state.fields.getOrElse(LeiTags.RegistrationStatus, ""),
              nextRenewalDate = state.fields.getOrElse(LeiTags.NextRenewalDate, "")
            ))
          else None

        (next, maybeRecord)
      .collect { case Some(r) => r }
      .grouped(50_000)
      .mapPar(4): chunk =>
        val count    = Database.insertChunk(chunk, config.dbUrl, config.dbUser, config.dbPassword)
        val newTotal = totalInserted.addAndGet(count)
        val ts       = LocalDateTime.now(ZoneId.systemDefault).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        println(s"    Total inserted: $newTotal records [$ts]")
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
      xmlFile
    else
      println(s"  [${fileType.name}] Downloading ZIP from ${fileType.url}...")
      downloadFile(fileType.url, zipFile)
      println(s"  [${fileType.name}] Extracting ZIP...")
      extractZip(zipFile, xmlFile)
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

  def   downloadXsd(dataDir: Path, fileType: GleifFileType): Unit =
    val xsdFileName = fileType.xsdUrl.split("/").last
    val xsdFile     = dataDir / xsdFileName
    if os.exists(xsdFile) then
      println(s"  [XSD] Using cached: ${xsdFile.last}")
    else
      println(s"  [XSD] Downloading XSD schema...")
      downloadFile(fileType.xsdUrl, xsdFile)
      println(s"  [XSD] Saved to ${xsdFile.last}")

  def extractZip(zipPath: Path, targetXml: Path): Unit =
    val tempDir = zipPath / os.up / "temp_extract"
    os.makeDir.all(tempDir)
    os.unzip(zipPath, tempDir)
    os.list(tempDir).find(_.last.endsWith(".xml")) match
      case Some(xml) => os.move.over(xml, targetXml)
      case None      => println("    WARNING: No XML file found in ZIP")
    os.remove.all(tempDir)

  // ── XML streaming ───────────────────────────────────────────────────────────

  /** Wraps a StAX reader into a lazy [[Flow]] of [[XmlEvent]]s using Aalto XML for better performance.
    */
  def xmlFlow(file: Path): Flow[XmlEvent] =
    val inputFactory = new InputFactoryImpl()

    Flow.usingEmit: emit =>
      val stream = FileInputStream(file.toString)
      val reader = inputFactory.createXMLStreamReader(stream)
      try
        while reader.hasNext do
          reader.next match
            case C.START_ELEMENT =>
              val attrs: Map[String, String] = (0 until reader.getAttributeCount)
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

// ── XML event model ──────────────────────────────────────────────────────────
enum XmlEvent:
  case Open(name: String, attrs: Map[String, String])
  case Close(name: String)
  case Text(value: String)

// ── XSD-derived constants for LEI-CDF v3.1 ─────────────────────────────────
object LeiTags:
  val LEIRecord               = "LEIRecord"
  val LEI                     = "LEI"
  val LegalName               = "LegalName"
  val LegalNameLang           = "LegalName@lang"
  val InitialRegistrationDate = "InitialRegistrationDate"
  val LastUpdateDate          = "LastUpdateDate"
  val RegistrationStatus      = "RegistrationStatus"
  val NextRenewalDate         = "NextRenewalDate"

case class Config(
    dataDir: String = "./data",
    dbUrl: String = "jdbc:postgresql://localhost:5434/gleif",
    dbUser: String = "esap_user",
    dbPassword: String = "esap_password"
)

enum DeltaType(val param: String, val suffix: String):
  case LastMonth extends DeltaType("LastMonth", "delta-last-month")
  case LastWeek  extends DeltaType("LastWeek", "delta-last-week")
  case LastDay   extends DeltaType("LastDay", "delta-last-day")
  case IntraDay  extends DeltaType("IntraDay", "delta-intraday")

enum GleifFileType(val name: String, val url: String, val xsdUrl: String):
  case GoldenCopy                            extends GleifFileType(
        "lei-golden-copy",
        "https://leidata-preview.gleif.org/api/v2/golden-copies/publishes/lei2/latest.xml",
        "https://www.gleif.org/lei-data/access-and-use-lei-data/level-1-data-lei-cdf-3-1-format/2021-03-04_lei-cdf-v3-1.xsd"
      )
  case Concatenated                          extends GleifFileType(
        "lei-cdf-concatenated",
        "https://leidata.gleif.org/api/v1/concatenated-files/lei2/latest/zip",
        "https://www.gleif.org/lei-data/access-and-use-lei-data/level-1-data-lei-cdf-3-1-format/2021-03-04_lei-cdf-v3-1.xsd"
      )
  case GoldenCopyDelta(deltaType: DeltaType) extends GleifFileType(
        s"lei-golden-copy-${deltaType.suffix}",
        s"https://leidata-preview.gleif.org/api/v2/golden-copies/publishes/lei2/latest.xml?delta=${deltaType.param}",
        "https://www.gleif.org/lei-data/access-and-use-lei-data/level-1-data-lei-cdf-3-1-format/2021-03-04_lei-cdf-v3-1.xsd"
      )
