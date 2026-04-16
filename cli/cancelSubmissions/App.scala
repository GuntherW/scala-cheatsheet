//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep com.lihaoyi::os-lib:0.11.9-M7
//> using file BlobService.scala
//> using file XmlProcessor.scala

import scala.io.StdIn

// ─── Pfade ───────────────────────────────────────────────────────────────────

val baseDir         = os.pwd
val archiveOutgoing = baseDir / "archive" / "outgoing"
val localOutgoing   = baseDir / "outgoing"
val sequenceFile    = (baseDir / "sequence.txt").toString

val remotePrefixArchive  = "archive/outgoing/"
val remotePrefixOutgoing = "outgoing/"

// ─── Menü-Hilfsfunktionen ────────────────────────────────────────────────────

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
  println(s"\n[Schritt 1] Download von esapsdeunr/$remotePrefixArchive ($mode)")
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

  val (ok, error) = blobs.foldLeft((0, 0)) { case ((ok, err), blob) =>
    print(s"  Lade herunter: ${blob.name} ... ")
    try
      downloadBlob(client, blob.path, archiveOutgoing.toString)
      println("OK")
      (ok + 1, err)
    catch
      case e: Exception =>
        println(s"FEHLER: ${e.getMessage}")
        (ok, err + 1)
  }

  printSeparator()
  println(s"Download abgeschlossen: $ok erfolgreich, $error fehlgeschlagen.")

// ─── Schritt 2: Process ──────────────────────────────────────────────────────

case class ProcessLogEntry(originalZip: String, newXmlName: String, newZipName: String, oldSeq: Int, newSeq: Int, status: String, message: String)

def writeProcessedMd(entries: List[ProcessLogEntry], timestamp: String): Unit =
  def orDash(s: String)  = if s.nonEmpty then s else "-"
  def seqOrDash(n: Int)  = if n > 0 then n.toString else "-"

  val header = "| Original-ZIP | Neue XML | Neue ZIP | Seq alt | Seq neu | Status | Hinweis |"
  val divider = "|---|---|---|---|---|---|---|"
  val rows = entries.map { e =>
    s"| ${e.originalZip} | ${orDash(e.newXmlName)} | ${orDash(e.newZipName)} | ${seqOrDash(e.oldSeq)} | ${seqOrDash(e.newSeq)} | ${e.status} | ${e.message.replace("|", "\\|")} |"
  }

  val content = (
    List(s"# Verarbeitungsprotokoll", "", s"**Zeitpunkt:** $timestamp", "", header, divider) ++ rows
  ).mkString("\n")

  os.write.over(baseDir / "processed.md", content)

def stepProcess(): Unit =
  println(s"\n[Schritt 2] Verarbeitung der ZIPs in $archiveOutgoing")
  printSeparator()

  os.makeDir.all(localOutgoing)

  val zipFiles = os.list(archiveOutgoing).filter(_.ext == "zip").sorted
  if zipFiles.isEmpty then
    println("Keine ZIP-Dateien in archive/outgoing/ gefunden.")
    return

  println(s"${zipFiles.size} ZIP(s) gefunden:")
  zipFiles.foreach(f => println(s"  - ${f.last}"))
  println()

  case class Acc(ok: Int, skipped: Int, error: Int, log: List[ProcessLogEntry])

  val result = zipFiles.foldLeft(Acc(0, 0, 0, Nil)) { (acc, zipPath) =>
    print(s"  Verarbeite: ${zipPath.last} ... ")
    processZip(zipPath.toString, localOutgoing.toString, sequenceFile) match
      case Left(msg) if msg.contains("übersprungen") =>
        println("UEBERSPRUNGEN")
        println(s"    Grund:    $msg")
        acc.copy(skipped = acc.skipped + 1, log = acc.log :+ ProcessLogEntry(zipPath.last, "", "", 0, 0, "UEBERSPRUNGEN", msg))
      case Left(msg) =>
        println(s"FEHLER: $msg")
        acc.copy(error = acc.error + 1, log = acc.log :+ ProcessLogEntry(zipPath.last, "", "", 0, 0, "FEHLER", msg))
      case Right(r) =>
        println("OK")
        println(s"    XML:      ${r.newXmlName}")
        println(s"    ZIP:      ${r.newZipName}")
        println(s"    Sequenz:  ${r.oldSequence} -> ${r.newSequence}")
        acc.copy(ok = acc.ok + 1, log = acc.log :+ ProcessLogEntry(r.originalZip, r.newXmlName, r.newZipName, r.oldSequence, r.newSequence, "OK", ""))
  }

  val timestamp = java.time.LocalDateTime.now().toString.replace("T", " ").take(19)
  writeProcessedMd(result.log, timestamp)
  println(s"\n  Protokoll gespeichert: processed.md")

  printSeparator()
  println(s"Verarbeitung abgeschlossen: ${result.ok} erfolgreich, ${result.skipped} uebersprungen, ${result.error} fehlgeschlagen.")

// ─── Schritt 3: Upload ───────────────────────────────────────────────────────

def stepUpload(mode: StorageMode): Unit =
  println(s"\n[Schritt 3] Upload von $localOutgoing nach esapsdeunr/$remotePrefixOutgoing ($mode)")
  printSeparator()

  val zipFiles = os.list(localOutgoing).filter(_.ext == "zip").sorted
  if zipFiles.isEmpty then
    println("Keine ZIP-Dateien in outgoing/ gefunden.")
    return

  println(s"${zipFiles.size} ZIP(s) gefunden:")
  zipFiles.foreach(f => println(s"  - ${f.last}"))
  println()

  val client = createBlobServiceClient(mode)

  val (ok, error) = zipFiles.foldLeft((0, 0)) { case ((ok, err), zipPath) =>
    val blobPath = s"$remotePrefixOutgoing${zipPath.last}"
    print(s"  Lade hoch: ${zipPath.last} -> $blobPath ... ")
    try
      uploadBlob(client, blobPath, zipPath.toString)
      println("OK")
      (ok + 1, err)
    catch
      case e: Exception =>
        println(s"FEHLER: ${e.getMessage}")
        (ok, err + 1)
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

  LazyList.continually(chooseStep())
    .takeWhile(_ != 0)
    .foreach {
      case 1 => stepDownload(mode)
      case 2 => stepProcess()
      case 3 => stepUpload(mode)
    }

  println("\nBeendet.")
