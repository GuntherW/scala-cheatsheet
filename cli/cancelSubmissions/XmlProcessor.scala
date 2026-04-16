import java.io.{File, FileInputStream, FileOutputStream, StringReader, StringWriter}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.{OutputKeys, TransformerFactory}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathFactory
import org.w3c.dom.{Document, NodeList}
import org.xml.sax.InputSource
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

// ─── Ergebnis-Typen ──────────────────────────────────────────────────────────

case class ProcessResult(
  originalZip: String,
  newZipName: String,
  newXmlName: String,
  oldSequence: Int,
  newSequence: Int
)

// ─── Hilfsfunktionen ─────────────────────────────────────────────────────────

/** Liest die aktuelle Sequenznummer aus sequence.txt */
def readSequence(sequenceFile: String): Try[Int] = Try {
  val content = scala.io.Source.fromFile(sequenceFile).mkString.trim
  content.toInt
}

/** Schreibt die neue Sequenznummer in sequence.txt */
def writeSequence(sequenceFile: String, seq: Int): Try[Unit] = Try {
  val fw = new java.io.FileWriter(sequenceFile)
  fw.write(seq.toString)
  fw.close()
}

/**
 * Entfernt einen angehängten Zeitstempel-Suffix der Form "_YYYYMMDDHHMMSS" vom Dateinamen.
 * Beispiel: "DEUNR_DATTAD_ESAPS_39120001KULK7200U106-00105_26_20260415060537" →
 *           "DEUNR_DATTAD_ESAPS_39120001KULK7200U106-00105_26"
 */
def stripTimestampSuffix(baseName: String): String =
  val timestampPattern = """_\d{14}$""".r
  timestampPattern.replaceFirstIn(baseName, "")

/**
 * Identifiziert die Haupt-XML-Datei in einem ZIP.
 * Die XML-Datei hat denselben Präfix wie die ZIP-Datei (ohne .zip und ohne Zeitstempel-Suffix).
 */
def findMainXmlEntry(zipFile: ZipFile, zipBaseName: String): Option[java.util.zip.ZipEntry] =
  val prefix = stripTimestampSuffix(zipBaseName)
  zipFile
    .entries()
    .asScala
    .find(e => !e.isDirectory && e.getName.endsWith(".xml") && e.getName.startsWith(prefix))

/**
 * Liest den Inhalt eines ZIP-Eintrags als String.
 */
def readZipEntry(zipFile: ZipFile, entry: java.util.zip.ZipEntry): String =
  val stream = zipFile.getInputStream(entry)
  val bytes  = stream.readAllBytes()
  stream.close()
  new String(bytes, java.nio.charset.StandardCharsets.UTF_8)

/**
 * Parst XML-Inhalt zu einem DOM-Document.
 */
def parseXml(xmlContent: String): Document =
  val factory = DocumentBuilderFactory.newInstance()
  factory.setNamespaceAware(true)
  val builder = factory.newDocumentBuilder()
  builder.parse(new InputSource(new StringReader(xmlContent)))

/**
 * Serialisiert ein DOM-Document zu einem XML-String.
 */
def serializeXml(doc: Document): String =
  val transformer = TransformerFactory.newInstance().newTransformer()
  transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
  transformer.setOutputProperty(OutputKeys.INDENT, "yes")
  val writer = new StringWriter()
  transformer.transform(new DOMSource(doc), new StreamResult(writer))
  writer.toString

/**
 * Sucht nach einem Element mit localName (namespace-unabhängig) und gibt dessen Textwert zurück.
 */
def findElementByLocalName(doc: Document, localName: String): Option[org.w3c.dom.Element] =
  val allElements = doc.getElementsByTagNameNS("*", localName)
  if allElements.getLength > 0 then Some(allElements.item(0).asInstanceOf[org.w3c.dom.Element])
  else None

/**
 * Extrahiert die Sequenz-Nummer aus einem BizMsgIdr-Wert.
 * Format: 39120001KULK7200U106-00107_26 → "00107" (fünfstellig vor dem letzten Unterstrich)
 * Regex: (\d{5})(?=_\d+$)
 */
def extractSequenceFromBizMsgIdr(bizMsgIdr: String): Option[String] =
  val pattern = """(\d{5})(?=_\d+$)""".r
  pattern.findFirstIn(bizMsgIdr)

/**
 * Ersetzt die Sequenz-Nummer in einem BizMsgIdr-Wert.
 */
def replaceSequenceInBizMsgIdr(bizMsgIdr: String, newSeq: String): String =
  val pattern = """(\d{5})(?=_\d+$)""".r
  pattern.replaceFirstIn(bizMsgIdr, newSeq)

/**
 * Formatiert eine Sequenznummer als 5-stelligen String mit führenden Nullen.
 */
def formatSequence(seq: Int): String = f"$seq%05d"

/**
 * Verarbeitet eine einzelne ZIP-Datei aus archive/outgoing:
 * 1. Entpackt die XML-Datei
 * 2. Ändert SubmissnTp → EROR (falls noch nicht EROR)
 * 3. Ändert BizMsgIdr mit neuer Sequenznummer
 * 4. Benennt die XML-Datei um
 * 5. Zippt die XML in outgoing/
 * 6. Inkrementiert sequence.txt
 *
 * Gibt Left(Fehlermeldung) bei Fehler oder bei bereits vorhandenem EROR zurück.
 * Gibt Right(ProcessResult) bei Erfolg zurück.
 */
def processZip(
  localZipPath: String,
  outgoingDir: String,
  sequenceFile: String
): Either[String, ProcessResult] =
  val zipFile = new File(localZipPath)
  val zipBaseName    = zipFile.getName.stripSuffix(".zip")
  val xmlPrefixClean = stripTimestampSuffix(zipBaseName)

  val zip = new ZipFile(zipFile)
  try
    // 1. Haupt-XML finden (Zeitstempel-Suffix wird ignoriert)
    val xmlEntry = findMainXmlEntry(zip, zipBaseName) match
      case None    => return Left(s"Keine XML mit Prefix '$xmlPrefixClean' in ${zipFile.getName} gefunden")
      case Some(e) => e

    // 2. XML lesen und parsen
    val xmlContent = readZipEntry(zip, xmlEntry)
    val doc        = parseXml(xmlContent)

    // 3. SubmissnTp prüfen
    val submissnTpElem = findElementByLocalName(doc, "SubmissnTp") match
      case None    => return Left(s"Tag 'SubmissnTp' nicht gefunden in ${xmlEntry.getName}")
      case Some(e) => e

    val currentSubmissnTp = submissnTpElem.getTextContent.trim
    if currentSubmissnTp == "EROR" then
      zip.close()
      return Left(s"SubmissnTp ist bereits EROR in ${xmlEntry.getName} – übersprungen")

    // 4. SubmissnTp auf EROR setzen
    submissnTpElem.setTextContent("EROR")

    // 5. BizMsgIdr lesen und Sequenz ersetzen
    val bizMsgIdrElem = findElementByLocalName(doc, "BizMsgIdr") match
      case None    => return Left(s"Tag 'BizMsgIdr' nicht gefunden in ${xmlEntry.getName}")
      case Some(e) => e

    val oldBizMsgIdr = bizMsgIdrElem.getTextContent.trim
    val oldSeqStr    = extractSequenceFromBizMsgIdr(oldBizMsgIdr) match
      case None    => return Left(s"Keine 5-stellige Sequenznummer in BizMsgIdr '$oldBizMsgIdr' gefunden")
      case Some(s) => s

    // 6. Neue Sequenznummer aus sequence.txt lesen
    val newSeqInt = readSequence(sequenceFile) match
      case Failure(e) => return Left(s"Fehler beim Lesen der sequence.txt: ${e.getMessage}")
      case Success(n) => n

    val newSeqStr    = formatSequence(newSeqInt)
    val newBizMsgIdr = replaceSequenceInBizMsgIdr(oldBizMsgIdr, newSeqStr)
    bizMsgIdrElem.setTextContent(newBizMsgIdr)

    // 7. Neuen XML-Dateinamen berechnen (Sequenz im Dateinamen ersetzen)
    val oldXmlName = xmlEntry.getName
    val newXmlName = replaceSequenceInBizMsgIdr(oldXmlName.stripSuffix(".xml"), newSeqStr) + ".xml"

    // 8. XML serialisieren
    val newXmlContent = serializeXml(doc)

    // 9. Neue ZIP in outgoing/ erstellen
    val newZipName = newXmlName.stripSuffix(".xml") + ".zip"
    val newZipPath = new File(outgoingDir, newZipName)

    val zos = new ZipOutputStream(new FileOutputStream(newZipPath))
    try
      zos.putNextEntry(new ZipEntry(newXmlName))
      zos.write(newXmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8))
      zos.closeEntry()
    finally
      zos.close()

    // 10. Sequenz inkrementieren
    writeSequence(sequenceFile, newSeqInt + 1) match
      case Failure(e) => return Left(s"Fehler beim Schreiben der sequence.txt: ${e.getMessage}")
      case Success(_) => ()

    Right(ProcessResult(
      originalZip = zipFile.getName,
      newZipName = newZipName,
      newXmlName = newXmlName,
      oldSequence = oldSeqStr.toInt,
      newSequence = newSeqInt
    ))
  finally
    zip.close()
