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
    val incomingPrefix = "archive/incoming/"
    val outgoingPrefix = "archive/outgoing/"
    val containerRows  =
      par(listAllContainers(blobClient).map(name => () => name -> loadBlobs(blobClient, name))).toList
        .sortBy(_._1.toLowerCase)

    val rows = containerRows.map { (containerName, blobs) =>
      val incomingCount = blobs.count(_.path.startsWith(incomingPrefix))
      val outgoingCount = blobs.count(_.path.startsWith(outgoingPrefix))
      s"| ${escapeMarkdownCell(containerName)} | $incomingCount | $outgoingCount |"
    }

    val markdown =
      Seq(
        "| Container | Files in archive/incoming | Files in archive/outgoing |",
        "| --- | ---: | ---: |"
      ) ++ rows

    os.write.over(reportPath, markdown.mkString("\n") + "\n")
    reportPath.toString
  }

  private def modeFilePart(mode: StorageMode): String = mode match
    case StorageMode.Azurite   => "azurite"
    case StorageMode.AzureTest => "azure-test"
    case StorageMode.AzureProd => "azure-prod"

  private def escapeMarkdownCell(value: String): String = value.replace("|", "\\|")
