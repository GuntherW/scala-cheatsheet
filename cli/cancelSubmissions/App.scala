//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep com.lihaoyi::os-lib:0.11.9-M7
//> using file BlobService.scala
//> using file XmlProcessor.scala

import java.io.File
import scala.io.StdIn

// ─── Pfade ───────────────────────────────────────────────────────────────────

val baseDir         = os.pwd
val archiveOutgoing = baseDir / "archive" / "outgoing"
val localOutgoing   = baseDir / "outgoing"
val sequenceFile    = (baseDir / "sequence.txt").toString

val remotePrefixArchive  = "archive/outgoing/"
val remotePrefixOutgoing = "outgoing/"

// ─── Menü-Hilfsfunktionen ────────────────────────────────────────────────────

def clearLine(): Unit = print("\r" + " " * 60 + "\r")

def printSeparator(): Unit = println("-" * 60)

def chooseStorageMode(): StorageMode =
  println()
  printSeparator()
  println("Umgebung waehlen:")
  println("  [1] Azurite  (lokaler Emulator)")
  println("  [2] AzureTest")
  println("  [3] AzureProd")
  printSeparator()
  print("Eingabe: ")
  StdIn.readLine().trim match
    case "1" => StorageMode.Azurite
    case "2" => StorageMode.AzureTest
    case "3" => StorageMode.AzureProd
    case _   =>
      println("Ungueltige Eingabe. Bitte 1, 2 oder 3 eingeben.")
      chooseStorageMode()

def chooseStep(): Int =
  println()
  printSeparator()
  println("Schritt waehlen:")
  println("  [1] Download   – ZIPs aus esapsdeunr/archive/outgoing runterladen")
  println("  [2] Process    – ZIPs verarbeiten, XML anpassen, neue ZIPs erstellen")
  println("  [3] Upload     – ZIPs aus lokalem outgoing/ hochladen")
  println("  [0] Beenden")
  printSeparator()
  print("Eingabe: ")
  StdIn.readLine().trim match
    case "1" => 1
    case "2" => 2
    case "3" => 3
    case "0" => 0
    case _   =>
      println("Ungueltige Eingabe. Bitte 0, 1, 2 oder 3 eingeben.")
      chooseStep()

// ─── Schritt 1: Download ─────────────────────────────────────────────────────

def stepDownload(mode: StorageMode): Unit =
  println(s"\n[Schritt 1] Download von esapsdeunr/${remotePrefixArchive} ($mode)")
  printSeparator()

  val client = createBlobServiceClient(mode)
  val blobs  = listBlobsWithPrefix(client, remotePrefixArchive)

  if blobs.isEmpty then
    println("Keine ZIPs im Remote-Verzeichnis gefunden.")
    return

  println(s"${blobs.size} Blob(s) gefunden:")
  blobs.foreach(b => println(s"  - ${b.name}  (${b.size} Bytes)"))
  println()

  os.makeDir.all(archiveOutgoing)

  var ok    = 0
  var error = 0
  blobs.foreach { blob =>
    print(s"  Lade herunter: ${blob.name} ... ")
    try
      downloadBlob(client, blob.path, archiveOutgoing.toString)
      println("OK")
      ok += 1
    catch
      case e: Exception =>
        println(s"FEHLER: ${e.getMessage}")
        error += 1
  }

  printSeparator()
  println(s"Download abgeschlossen: $ok erfolgreich, $error fehlgeschlagen.")

// ─── Schritt 2: Process ──────────────────────────────────────────────────────

case class ProcessLogEntry(originalZip: String, newXmlName: String, newZipName: String, oldSeq: Int, newSeq: Int, status: String, message: String)

def writeProcessedMd(entries: List[ProcessLogEntry], timestamp: String): Unit =
  val mdPath = baseDir / "processed.md"
  val sb     = new StringBuilder

  sb.append(s"# Verarbeitungsprotokoll\n\n")
  sb.append(s"**Zeitpunkt:** $timestamp\n\n")
  sb.append("| Original-ZIP | Neue XML | Neue ZIP | Seq alt | Seq neu | Status | Hinweis |\n")
  sb.append("|---|---|---|---|---|---|---|\n")

  entries.foreach { e =>
    val seqAlt  = if e.oldSeq > 0 then e.oldSeq.toString else "-"
    val seqNeu  = if e.newSeq > 0 then e.newSeq.toString else "-"
    val xmlName = if e.newXmlName.nonEmpty then e.newXmlName else "-"
    val zipName = if e.newZipName.nonEmpty then e.newZipName else "-"
    val msg     = e.message.replace("|", "\\|")
    sb.append(s"| ${e.originalZip} | $xmlName | $zipName | $seqAlt | $seqNeu | ${e.status} | $msg |\n")
  }

  os.write.over(mdPath, sb.toString)

def stepProcess(): Unit =
  println(s"\n[Schritt 2] Verarbeitung der ZIPs in ${archiveOutgoing}")
  printSeparator()

  os.makeDir.all(localOutgoing)

  val zipFiles = os.list(archiveOutgoing).filter(_.ext == "zip").sorted
  if zipFiles.isEmpty then
    println("Keine ZIP-Dateien in archive/outgoing/ gefunden.")
    return

  println(s"${zipFiles.size} ZIP(s) gefunden:")
  zipFiles.foreach(f => println(s"  - ${f.last}"))
  println()

  var ok      = 0
  var skipped = 0
  var error   = 0
  val logEntries = scala.collection.mutable.ListBuffer[ProcessLogEntry]()

  zipFiles.foreach { zipPath =>
    print(s"  Verarbeite: ${zipPath.last} ... ")
    processZip(zipPath.toString, localOutgoing.toString, sequenceFile) match
      case Left(msg) if msg.contains("uebersprungen") =>
        println(s"UEBERSPRUNGEN")
        println(s"    Grund:    $msg")
        skipped += 1
        logEntries += ProcessLogEntry(zipPath.last, "", "", 0, 0, "UEBERSPRUNGEN", msg)
      case Left(msg) =>
        println(s"FEHLER: $msg")
        error += 1
        logEntries += ProcessLogEntry(zipPath.last, "", "", 0, 0, "FEHLER", msg)
      case Right(result) =>
        println(s"OK")
        println(s"    XML:      ${result.newXmlName}")
        println(s"    ZIP:      ${result.newZipName}")
        println(s"    Sequenz:  ${result.oldSequence} -> ${result.newSequence}")
        ok += 1
        logEntries += ProcessLogEntry(result.originalZip, result.newXmlName, result.newZipName, result.oldSequence, result.newSequence, "OK", "")
  }

  val timestamp = java.time.LocalDateTime.now().toString.replace("T", " ").take(19)
  writeProcessedMd(logEntries.toList, timestamp)
  println(s"\n  Protokoll gespeichert: processed.md")

  printSeparator()
  println(s"Verarbeitung abgeschlossen: $ok erfolgreich, $skipped uebersprungen, $error fehlgeschlagen.")

// ─── Schritt 3: Upload ───────────────────────────────────────────────────────

def stepUpload(mode: StorageMode): Unit =
  println(s"\n[Schritt 3] Upload von ${localOutgoing} nach esapsdeunr/${remotePrefixOutgoing} ($mode)")
  printSeparator()

  val zipFiles = os.list(localOutgoing).filter(_.ext == "zip").sorted
  if zipFiles.isEmpty then
    println("Keine ZIP-Dateien in outgoing/ gefunden.")
    return

  println(s"${zipFiles.size} ZIP(s) gefunden:")
  zipFiles.foreach(f => println(s"  - ${f.last}"))
  println()

  val client = createBlobServiceClient(mode)

  var ok    = 0
  var error = 0
  zipFiles.foreach { zipPath =>
    val blobPath = s"$remotePrefixOutgoing${zipPath.last}"
    print(s"  Lade hoch: ${zipPath.last} -> $blobPath ... ")
    try
      uploadBlob(client, blobPath, zipPath.toString)
      println("OK")
      ok += 1
    catch
      case e: Exception =>
        println(s"FEHLER: ${e.getMessage}")
        error += 1
  }

  printSeparator()
  println(s"Upload abgeschlossen: $ok erfolgreich, $error fehlgeschlagen.")

// ─── Hauptprogramm ───────────────────────────────────────────────────────────

@main
def main(): Unit =
  println("=" * 60)
  println("  cancelSubmissions – ESAP Submission Corrector")
  println("=" * 60)

  val mode = chooseStorageMode()
  println(s"\nUmgebung: $mode")

  var running = true
  while running do
    val step = chooseStep()
    step match
      case 1 => stepDownload(mode)
      case 2 => stepProcess()
      case 3 => stepUpload(mode)
      case 0 =>
        println("\nBeendet.")
        running = false
      case _ => ()
