import layoutz.*

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.Locale

object Renderer:

  def render(state: AppState, computeFlatItems: AppState => List[NodeView]): Element =
    if state.error.isDefined
    then renderError(state.error.get)
    else if state.pendingUpload.isDefined
    then renderUploadDialog(state)
    else renderMainView(state, computeFlatItems)

  private def renderError(error: String): Element =
    layout(s"❌ Fehler: $error")

  private def renderMainView(state: AppState, computeFlatItems: AppState => List[NodeView]): Element =
    val items         = computeFlatItems(state)
    val itemElements  = items.zipWithIndex.map(renderItem(state, computeFlatItems))
    val statusElement = renderStatus(state.statusMessage).center()
    val timeStr       = state.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.GERMANY)).color(Color.BrightBlack)

    val header = row(
      "Container".color(Color.BrightBlack),
      space(65),
      timeStr.color(Color.BrightBlack)
    )

    val helpText = buildHelpText(items, state.selectedIndex)

    layout(
      box("▶" + state.storageMode.toString)(
        header,
        "",
        Layout(itemElements),
        "",
        statusElement,
        "",
        helpText.color(Color.BrightBlack).center()
      ).color(Color.Green)
    )

  private def buildHelpText(items: List[NodeView], selectedIndex: Int): String =
    val base = "↑↓ Navigieren   Enter Öffnen/Download   m Modus   a Aktualisieren"
    items.lift(selectedIndex) match
      case Some(_: FileView) => s"$base   l Löschen"
      case Some(_: DirView)  => s"$base   u Upload"
      case None              => base

  private def renderItem(state: AppState, computeFlatItems: AppState => List[NodeView])(itemWithIndex: (NodeView, Int)): Element =
    val (item, idx) = itemWithIndex
    val isSelected  = idx == state.selectedIndex
    val line        = formatItemLine(item, isSelected, state)
    Text(line).color(if isSelected then Color.Yellow else Color.White)

  private def formatItemLine(item: NodeView, isSelected: Boolean, state: AppState): String =
    val indent        = "  " * item.depth
    val cursor        = if isSelected then "▶ " else "  "
    val (icon, label) = item match
      case d: DirView  =>
        val count    = countFiles(d)
        val sizeMB   = d.totalSize / (1024.0 * 1024.0)
        val countStr = if count > 0 then s" [$count]" else ""
        val sizeStr  = if sizeMB > 0 then s" (${f"$sizeMB%.2f"} MB)" else ""
        (directoryIcon(d, state), d.name + countStr + sizeStr)
      case f: FileView =>
        val sizeMB  = f.size / (1024.0 * 1024.0)
        val sizeStr = if sizeMB > 0 then s" (${f"$sizeMB%.2f"} MB)" else ""
        (fileIcon(f.name), f.name + sizeStr)
    s"$indent$cursor$icon$label"

  private def countFiles(dir: DirView): Int =
    def countRecursive(node: NodeView): Int = node match
      case _: FileView => 1
      case d: DirView  => d.children.values.iterator.map(countRecursive).sum
    countRecursive(dir)

  private def directoryIcon(dir: DirView, state: AppState): String =
    if dir.depth == 0 then "🗄️ "
    else if state.expandedPaths.contains(dir.fullPath) then "📂 "
    else "📁 "

  private def fileIcon(fileName: String): String =
    if fileName.endsWith(".zip")
    then "🗜️  "
    else "📎 "

  private def renderStatus(status: Option[StatusMessage]): Element = status match
    case Some(msg) => statusCard("Info", msg.text).border(Border.Thick).color(Color.BrightYellow)
    case None      => Text("")

  private def renderUploadDialog(state: AppState): Element =
    state.pendingUpload match
      case None         => layout("")
      case Some(upload) =>
        val itemElements = upload.localItems.zipWithIndex.map { (item, idx) =>
          val cursor = if idx == upload.localSelectedIndex then "▶ " else "  "
          val icon   = item match
            case _: DirLocal  => "📂 "
            case _: FileLocal => "📎 "
          val line   = s"$cursor$icon${item.name}"
          Text(line).color(if idx == upload.localSelectedIndex then Color.Yellow else Color.White)
        }
        layout(
          box("Datei hochladen")(
            Text(s"Container: ${upload.containerName}").color(Color.White),
            Text(s"Zielordner: ${upload.blobFolderPath}").color(Color.White),
            Text(s"Lokal: ${upload.localCurrentPath}").color(Color.White),
            Text("").color(Color.White),
            Layout(itemElements),
            Text("").color(Color.White),
            Text("↑↓ Navigieren   Enter = Öffnen/Hochladen   z = Zurück   q = Abbrechen").color(Color.BrightBlack)
          ).color(Color.Cyan)
        )
