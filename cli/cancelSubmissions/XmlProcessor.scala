import java.io.{File, FileOutputStream, FileWriter, StringReader, StringWriter}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.{OutputKeys, TransformerFactory}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.xml.sax.InputSource

import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

// ─── Ergebnis-Typen ──────────────────────────────────────────────────────────

case class ProcessResult(
    originalZip: String,
    newZipName: String,
    newXmlName: String,
    oldSequence: Int,
    newSequence: Int
)

// ─── Hilfsfunktionen ─────────────────────────────────────────────────────────

val sequencePattern = """(\d{5})(?=_\d+$)""".r

/** Liest die aktuelle Sequenznummer aus sequence.txt */
def readSequence(sequenceFile: String): Try[Int] =
  Using(Source.fromFile(sequenceFile))(_.mkString.trim.toInt)

/** Schreibt die neue Sequenznummer in sequence.txt */
def writeSequence(sequenceFile: String, seq: Int): Try[Unit] =
  Using(new FileWriter(sequenceFile))(_.write(seq.toString))

/** Entfernt einen angehängten Zeitstempel-Suffix der Form "_YYYYMMDDHHMMSS" vom Dateinamen. Beispiel: "DEUNR_DATTAD_ESAPS_39120001KULK7200U106-00105_26_20260415060537" →
  * "DEUNR_DATTAD_ESAPS_39120001KULK7200U106-00105_26"
  */
def stripTimestampSuffix(baseName: String): String =
  """_\d{14}$""".r.replaceFirstIn(baseName, "")

/** Identifiziert die Haupt-XML-Datei in einem ZIP. Der Zeitstempel-Suffix im ZIP-Dateinamen wird beim Vergleich ignoriert.
  */
def findMainXmlEntry(zipFile: ZipFile, zipBaseName: String): Option[ZipEntry] =
  val prefix = stripTimestampSuffix(zipBaseName)
  zipFile
    .entries()
    .asScala
    .find(e => !e.isDirectory && e.getName.endsWith(".xml") && e.getName.startsWith(prefix))

/** Liest den Inhalt eines ZIP-Eintrags als String. */
def readZipEntry(zipFile: ZipFile, entry: ZipEntry): String =
  Using(zipFile.getInputStream(entry)) { stream =>
    new String(stream.readAllBytes(), UTF_8)
  }.get

/** Parst XML-Inhalt zu einem DOM-Document. */
def parseXml(xmlContent: String): Document =
  val factory = DocumentBuilderFactory.newInstance()
  factory.setNamespaceAware(true)
  factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)))

/** Serialisiert ein DOM-Document zu einem XML-String. */
def serializeXml(doc: Document): String =
  val transformer = TransformerFactory.newInstance().newTransformer()
  transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
  transformer.setOutputProperty(OutputKeys.INDENT, "yes")
  val writer      = new StringWriter()
  transformer.transform(new DOMSource(doc), new StreamResult(writer))
  writer.toString

/** Sucht ein Element anhand seines localName (namespace-unabhängig). */
def findElementByLocalName(doc: Document, localName: String): Option[org.w3c.dom.Element] =
  val elements = doc.getElementsByTagNameNS("*", localName)
  Option.when(elements.getLength > 0)(elements.item(0).asInstanceOf[org.w3c.dom.Element])

/** Extrahiert die 5-stellige Sequenz aus einem BizMsgIdr-Wert. */
def extractSequence(bizMsgIdr: String): Option[String] =
  sequencePattern.findFirstIn(bizMsgIdr)

/** Ersetzt die 5-stellige Sequenz in einem BizMsgIdr-Wert oder Dateinamen. */
def replaceSequence(value: String, newSeq: String): String =
  sequencePattern.replaceFirstIn(value, newSeq)

/** Formatiert eine Sequenznummer als 5-stelligen String mit führenden Nullen. */
def formatSequence(seq: Int): String = f"$seq%05d"

/** Verarbeitet eine einzelne ZIP-Datei aus archive/outgoing:
  *   1. Entpackt die Haupt-XML (Zeitstempel im ZIP-Namen wird ignoriert)
  *   2. Setzt SubmissnTp → EROR (bricht ab falls bereits EROR)
  *   3. Ersetzt Sequenz in BizMsgIdr und Dateiname
  *   4. Zippt die geänderte XML nach outgoing/
  *   5. Inkrementiert sequence.txt
  */
def processZip(
    localZipPath: String,
    outgoingDir: String,
    sequenceFile: String
): Either[String, ProcessResult] =
  val zipFile        = new File(localZipPath)
  val zipBaseName    = zipFile.getName.stripSuffix(".zip")
  val xmlPrefixClean = stripTimestampSuffix(zipBaseName)

  Using(new ZipFile(zipFile)) { zip =>
    for
      xmlEntry       <- findMainXmlEntry(zip, zipBaseName).toRight(s"Keine XML mit Prefix '$xmlPrefixClean' in ${zipFile.getName} gefunden")
      doc             = parseXml(readZipEntry(zip, xmlEntry))
      submissnTpElem <- findElementByLocalName(doc, "SubmissnTp").toRight(s"Tag 'SubmissnTp' nicht gefunden in ${xmlEntry.getName}")
      _              <- Either.cond(submissnTpElem.getTextContent.trim != "EROR", (), s"SubmissnTp ist bereits EROR in ${xmlEntry.getName} – übersprungen")
      _               = submissnTpElem.setTextContent("EROR")
      bizMsgIdrElem  <- findElementByLocalName(doc, "BizMsgIdr").toRight(s"Tag 'BizMsgIdr' nicht gefunden in ${xmlEntry.getName}")
      oldBizMsgIdr    = bizMsgIdrElem.getTextContent.trim
      oldSeqStr      <- extractSequence(oldBizMsgIdr).toRight(s"Keine 5-stellige Sequenznummer in BizMsgIdr '$oldBizMsgIdr' gefunden")
      newSeqInt      <- readSequence(sequenceFile).toEither.left.map(e => s"Fehler beim Lesen der sequence.txt: ${e.getMessage}")
      newSeqStr       = formatSequence(newSeqInt)
      _               = bizMsgIdrElem.setTextContent(replaceSequence(oldBizMsgIdr, newSeqStr))
      newXmlName      = replaceSequence(xmlEntry.getName.stripSuffix(".xml"), newSeqStr) + ".xml"
      newZipName      = newXmlName.stripSuffix(".xml") + ".zip"
      _              <- writeZip(new File(outgoingDir, newZipName), newXmlName, serializeXml(doc))
      _              <- writeSequence(sequenceFile, newSeqInt + 1).toEither.left.map(e => s"Fehler beim Schreiben der sequence.txt: ${e.getMessage}")
    yield ProcessResult(zipFile.getName, newZipName, newXmlName, oldSeqStr.toInt, newSeqInt)
  }.toEither.left.map(_.getMessage).flatten

/** Schreibt eine einzelne XML-Datei als ZIP. */
def writeZip(zipFile: File, entryName: String, content: String): Either[String, Unit] =
  Try {
    Using(new ZipOutputStream(new FileOutputStream(zipFile))) { zos =>
      zos.putNextEntry(new ZipEntry(entryName))
      zos.write(content.getBytes(UTF_8))
      zos.closeEntry()
    }.get
  }.toEither.left.map(e => s"Fehler beim Schreiben der ZIP ${zipFile.getName}: ${e.getMessage}")
