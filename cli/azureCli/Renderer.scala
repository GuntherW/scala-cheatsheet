import layoutz.*

object Renderer:

  def render(state: BlobState, computeFlatItems: BlobState => List[FileNode]): Element =
    if state.error.nonEmpty
    then renderError(state.error.get)
    else if state.pendingUpload.isDefined
    then renderUploadDialog(state)
    else renderMainView(state, computeFlatItems)

  private def renderError(error: String): Element =
    layout(s"❌ Fehler: $error")

  private def renderMainView(state: BlobState, computeFlatItems: BlobState => List[FileNode]): Element =
    val items         = computeFlatItems(state)
    val itemElements  = items.zipWithIndex.map(renderItem(state, computeFlatItems))
    val statusElement = renderStatus(state.downloadStatus)

    layout(
      box(s"${state.storageMode}")(
        Layout(itemElements),
        "",
        statusElement,
        "",
        state.time.toString.color(Color.BrightBlack).center(),
        "",
        Text("↑↓ Navigieren   Enter Öffnen/Download   l Löschen   u Upload   m Modus   a Aktualisieren").color(Color.BrightBlack).center()
      ).color(Color.Green)
    )

  private def renderItem(state: BlobState, computeFlatItems: BlobState => List[FileNode])(itemWithIndex: (FileNode, Int)): Element =
    val (item, idx) = itemWithIndex
    val isSelected  = idx == state.selectedIndex
    val line        = formatItemLine(item, isSelected, state)
    Text(line).color(if isSelected then Color.Yellow else Color.White)

  private def formatItemLine(item: FileNode, isSelected: Boolean, state: BlobState): String =
    val indent        = "  " * item.depth
    val cursor        = if isSelected then "▶ " else "  "
    val (icon, label) = item match
      case d: Directory =>
        val count    = countFiles(d)
        val countStr = if count > 0 then s" [$count]" else ""
        (directoryIcon(d, state), d.name + countStr)
      case f: File      => (fileIcon(f.name), f.name)
    s"$indent$cursor$icon$label"

  private def countFiles(dir: Directory): Int =
    def countRecursive(node: FileNode): Int = node match
      case _: File      => 1
      case d: Directory => d.children.values.iterator.map(countRecursive).sum
    countRecursive(dir)

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

  private def renderUploadDialog(state: BlobState): Element =
    val upload       = state.pendingUpload.get
    val itemElements = upload.localItems.zipWithIndex.map { (item, idx) =>
      val cursor = if idx == upload.localSelectedIndex then "▶ " else "  "
      val icon   = item match
        case _: LocalDir  => "📂 "
        case _: LocalFile => "📎 "
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
