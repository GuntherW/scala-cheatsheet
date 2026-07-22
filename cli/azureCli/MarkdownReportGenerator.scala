import com.azure.storage.blob.BlobServiceClient
import ox.*

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

object MarkdownReportGenerator:

  def generate(storageMode: StorageMode, blobClient: BlobServiceClient): Try[String] = Try {
    val timestamp      = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    val modePart       = modeFilePart(storageMode)
    val reportPath     = os.pwd / s"container-report-$modePart-$timestamp.md"
    val prefixArchiveIncoming = "archive/incoming/"
    val prefixArchiveOutgoing = "archive/outgoing/"
    val prefixIncoming   = "incoming/"
    val prefixOutgoing   = "outgoing/"
    val containerRows  =
      par(listAllContainers(blobClient).map(name => () => name -> loadBlobs(blobClient, name))).toList
        .sortBy(_._1.toLowerCase)

    val rows = containerRows.map { (containerName, blobs) =>
      val archiveIncomingCount = blobs.count(_.path.startsWith(prefixArchiveIncoming))
      val archiveOutgoingCount = blobs.count(_.path.startsWith(prefixArchiveOutgoing))
      val hubexIncomingCount   = blobs.count(_.path.startsWith(prefixIncoming))
      val hubexOutgoingCount   = blobs.count(_.path.startsWith(prefixOutgoing))
      s"| ${escapeMarkdownCell(containerName)} | $archiveIncomingCount | $archiveOutgoingCount | $hubexIncomingCount | $hubexOutgoingCount |"
    }

    val markdown =
      Seq(
        "| Container | Files in archive/incoming | Files in archive/outgoing | Files in /incoming | Files in /outgoing |",
        "| --- | ---: | ---: | ---: | ---: |"
      ) ++ rows

    os.write.over(reportPath, markdown.mkString("\n") + "\n")
    reportPath.toString
  }

  private def modeFilePart(mode: StorageMode): String = mode match
    case StorageMode.Azurite   => "azurite"
    case StorageMode.AzureTest => "azure-test"
    case StorageMode.AzureProd => "azure-prod"

  private def escapeMarkdownCell(value: String): String = value.replace("|", "\\|")
