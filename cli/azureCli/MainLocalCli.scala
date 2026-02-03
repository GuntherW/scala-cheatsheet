//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz::0.6.0
//> using file AzureBlobService.scala

import scala.util.{Failure, Success, Try}
import layoutz.*

import java.time.LocalDateTime

@main
def mainLocalCli(): Unit = BlobViewerApp.run()

case class BlobState(
    blobsIncoming: List[BlobInfo] = Nil,
    blobsOutgoing: List[BlobInfo] = Nil,
    time: LocalDateTime = LocalDateTime.now,
    error: Option[String] = None
)

enum BlobViewMsg:
  case LoadError(error: String)
  case Refresh

object BlobViewerApp extends LayoutzApp[BlobState, BlobViewMsg] {

  private def loadAllBlobs(): (BlobState, Cmd[BlobViewMsg]) =
    Try((loadBlobs("incoming"), loadBlobs("outgoing"))) match
      case Success((blobsIncoming, blobsOutgoing)) => (BlobState(blobsIncoming, blobsOutgoing, LocalDateTime.now), Cmd.none)
      case Failure(error)                          => (BlobState(time = LocalDateTime.now, error = Some(error.getMessage)), Cmd.none)

  def init = loadAllBlobs()

  def update(msg: BlobViewMsg, state: BlobState) = msg match
    case BlobViewMsg.LoadError(error) => (state.copy(error = Some(error)), Cmd.none)
    case BlobViewMsg.Refresh          => loadAllBlobs()

  def subscriptions(state: BlobState) =
    Sub.batch(
      Sub.time.every(1000, BlobViewMsg.Refresh),
      Sub.onKeyPress {
        case CharKey('r') => Some(BlobViewMsg.Refresh)
        case _            => None
      }
    )

  def view(state: BlobState) =
    state.error match
      case Some(error) => layout(s"❌ Fehler: $error")
      case None        =>
        layout(
          box(config.accountName)(
            row(
              box("incoming")(
                s"   ${state.blobsIncoming.size} Dateien gefunden",
                "",
                renderTree(buildTreeStructure(state.blobsIncoming)).color(Color.Cyan)
              ).color(Color.Blue),
              box("outgoing")(
                s"   ${state.blobsOutgoing.size} Dateien gefunden",
                "",
                renderTree(buildTreeStructure(state.blobsOutgoing)).color(Color.Cyan)
              ).color(Color.Blue)
            ),
            "",
            state.time.toString.color(Color.BrightBlack).center()
          ).color(Color.Green)
        )

  def renderTree(node: FileNode): TreeNode = node match
    case Directory(name, children) => tree(s"📁 $name")(children.values.map(renderTree).toSeq*)
    case File(name)                => tree(s"${if name.endsWith(".zip") then "🗜️" else "📎"} $name")
}
