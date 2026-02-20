//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz:0.6.0
//> using file BlobService.scala
//> using file Model.scala

import scala.util.{Failure, Success, Try}
import layoutz.*
import java.time.LocalDateTime

@main
def mainLocalCli(): Unit = BlobViewerApp.run()

object BlobViewerApp extends LayoutzApp[BlobState, BlobViewMsg]:

  def init: (BlobState, Cmd[BlobViewMsg]) = loadContainerData() match
    case Success(data) => (BlobState(containers = data, time = LocalDateTime.now), Cmd.none)
    case Failure(e)    => (BlobState(time = LocalDateTime.now, error = Some(e.getMessage)), Cmd.none)

  def update(msg: BlobViewMsg, state: BlobState): (BlobState, Cmd[BlobViewMsg]) = msg match

    case BlobViewMsg.LoadError(error) => (state.copy(error = Some(error)), Cmd.none)

    case BlobViewMsg.Refresh =>
      loadContainerData() match
        case Success(data) => (state.copy(containers = data, time = LocalDateTime.now, error = None), Cmd.none)
        case Failure(e)    => (state.copy(error = Some(e.getMessage), time = LocalDateTime.now), Cmd.none)

    case BlobViewMsg.MoveUp =>
      val newIndex = (state.selectedIndex - 1).max(0)
      (state.copy(selectedIndex = newIndex), Cmd.none)

    case BlobViewMsg.MoveDown =>
      val items    = computeFlatItems(state)
      val maxIndex = math.max(0, items.length - 1)
      val newIndex = (state.selectedIndex + 1).min(maxIndex)
      (state.copy(selectedIndex = newIndex), Cmd.none)

    case BlobViewMsg.Select => selectItem(state)

  def subscriptions(state: BlobState): Sub[BlobViewMsg] = Sub.batch(
    Sub.time.every(5000, BlobViewMsg.Refresh),
    Sub.onKeyPress {
      case ArrowUpKey   => Some(BlobViewMsg.MoveUp)
      case ArrowDownKey => Some(BlobViewMsg.MoveDown)
      case EnterKey     => Some(BlobViewMsg.Select)
      case CharKey('r') => Some(BlobViewMsg.Refresh)
      case _            => None
    }
  )

  def view(state: BlobState): Element =
    if state.error.nonEmpty
    then renderError(state.error.get)
    else renderMainView(state)

  private def loadContainerData(): Try[Map[String, List[BlobInfo]]] = Try {
    val containers = listContainers()
    containers.map(name => name -> loadBlobs(name)).toMap
  }

  private def selectItem(state: BlobState): (BlobState, Cmd[BlobViewMsg]) =
    val items = computeFlatItems(state)
    items.lift(state.selectedIndex) match
      case Some(d: Directory) => toggleDirectory(state, d)
      case Some(f: File)      => downloadFile(state, f)
      case None               => (state, Cmd.none)

  private def toggleDirectory(state: BlobState, dir: Directory): (BlobState, Cmd[BlobViewMsg]) =
    val key         = pathKey(dir.containerName, dir.path)
    val newExpanded =
      if state.expandedPaths.contains(key)
      then state.expandedPaths - key
      else state.expandedPaths + key
    (state.copy(expandedPaths = newExpanded), Cmd.none)

  private def downloadFile(state: BlobState, file: File): (BlobState, Cmd[BlobViewMsg]) =
    Try(downloadBlob(file.containerName, file.blobPath)) match
      case Success(targetPath) =>
        val status = s"✅ ${file.name}  →  $targetPath"
        (state.copy(downloadStatus = Some(status)), Cmd.none)
      case Failure(e)          =>
        val status = s"❌ Download fehlgeschlagen: ${e.getMessage}"
        (state.copy(downloadStatus = Some(status)), Cmd.none)

  private def computeFlatItems(state: BlobState): List[FileNode] =
    state.containers.toList.sortBy(_._1).flatMap { (containerName, blobs) =>
      val root = buildTreeStructure(containerName, blobs)
      flattenNode(root, state.expandedPaths)
    }

  private def flattenNode(node: FileNode, expandedPaths: Set[String]): List[FileNode] = node match
    case file: File     => List(file)
    case dir: Directory =>
      val key = pathKey(dir.containerName, dir.path)
      if expandedPaths.contains(key) then dir :: dir.children.values.toList.sortBy(_.name).flatMap(child => flattenNode(child, expandedPaths))
      else List(dir)

  private def pathKey(containerName: String, path: String): String = s"$containerName:$path"

  private def renderError(error: String): Element = layout(s"❌ Fehler: $error")

  private def renderMainView(state: BlobState): Element =
    val items         = computeFlatItems(state)
    val itemElements  = items.zipWithIndex.map(renderItem(state))
    val statusElement = renderStatus(state.downloadStatus)

    layout(
      box(config.accountName)(
        Layout(itemElements),
        "",
        statusElement,
        "",
        state.time.toString.color(Color.BrightBlack).center(),
        "",
        Text("↑↓ Navigieren   Enter Öffnen/Download   r Aktualisieren").color(Color.BrightBlack).center()
      ).color(Color.Green)
    )

  private def renderItem(state: BlobState)(itemWithIndex: (FileNode, Int)): Element =
    val (item, idx) = itemWithIndex
    val isSelected  = idx == state.selectedIndex
    val line        = formatItemLine(item, isSelected, state)
    Text(line).color(if isSelected then Color.Yellow else Color.White)

  private def formatItemLine(item: FileNode, isSelected: Boolean, state: BlobState): String =
    val indent        = "  " * item.depth
    val cursor        = if isSelected then "▶ " else "  "
    val (icon, label) = item match
      case d: Directory => (directoryIcon(d, state), d.name)
      case f: File      => (fileIcon(f.name), f.name)
    s"$indent$cursor$icon$label"

  private def directoryIcon(dir: Directory, state: BlobState): String =
    if dir.depth == 0
    then "🗄️  "
    else if state.expandedPaths.contains(pathKey(dir.containerName, dir.path)) then "📂 "
    else "📁 "

  private def fileIcon(fileName: String): String =
    if fileName.endsWith(".zip")
    then "🗜️  "
    else "📎 "

  private def renderStatus(status: Option[String]): Element = status match
    case Some(s) => Text(s).color(Color.BrightGreen)
    case None    => Text("")
