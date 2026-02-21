//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz:0.6.0
//> using dep com.lihaoyi::os-lib:0.11.9-M6
//> using file BlobService.scala
//> using file Model.scala
//> using file Renderer.scala

import scala.util.{Failure, Success, Try}
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import layoutz.*
import java.time.LocalDateTime

@main
def main(): Unit = BlobViewerApp.run()

object BlobViewerApp extends LayoutzApp[BlobState, BlobViewMsg]:

  def init: (BlobState, Cmd[BlobViewMsg]) =
    val mode = getStorageMode()
    loadContainerData(mode) match
      case Success(data) => (BlobState(containers = data, time = LocalDateTime.now, storageMode = mode), Cmd.none)
      case Failure(e)    => (BlobState(time = LocalDateTime.now, error = Some(e.getMessage), storageMode = mode), Cmd.none)

  def view(state: BlobState): Element =
    Renderer.render(state, computeFlatItems)

  def update(msg: BlobViewMsg, state: BlobState): (BlobState, Cmd[BlobViewMsg]) = msg match

    case BlobViewMsg.LoadError(error) => (state.copy(error = Some(error)), Cmd.none)

    case BlobViewMsg.Refresh =>
      if state.pendingUpload.isDefined then (state, Cmd.none)
      else
        loadContainerData(state.storageMode) match
          case Success(data) => (state.copy(containers = data, time = LocalDateTime.now, error = None, downloadStatus = None), Cmd.none)
          case Failure(e)    => (state.copy(error = Some(e.getMessage), time = LocalDateTime.now), Cmd.none)

    case BlobViewMsg.SwitchMode =>
      val newMode = state.storageMode match
        case StorageMode.Azurite => StorageMode.Azure
        case StorageMode.Azure   => StorageMode.Azurite
      loadContainerData(newMode) match
        case Success(data) =>
          val status = s"🔄 Modus gewechselt zu: $newMode"
          (state.copy(containers = data, storageMode = newMode, downloadStatus = Some(status)), Cmd.none)
        case Failure(e)    =>
          (state.copy(storageMode = newMode, error = Some(e.getMessage)), Cmd.none)

    case BlobViewMsg.MoveUp =>
      val newIndex = moveIndex(state.selectedIndex, -1, computeFlatItems(state).length)
      (state.copy(selectedIndex = newIndex), Cmd.none)

    case BlobViewMsg.MoveDown =>
      val newIndex = moveIndex(state.selectedIndex, 1, computeFlatItems(state).length)
      (state.copy(selectedIndex = newIndex), Cmd.none)

    case BlobViewMsg.Select => selectItem(state)

    case BlobViewMsg.DeleteFile =>
      val items = computeFlatItems(state)
      items.lift(state.selectedIndex) match
        case Some(f: File) =>
          Try(deleteBlob(f.containerName, f.blobPath)(state.storageMode)) match
            case Success(_) =>
              val newState = state.copy(downloadStatus = Some(s"✅ Gelöscht: ${f.name}"))
              loadContainerData(state.storageMode) match
                case Success(data) => (newState.copy(containers = data, time = LocalDateTime.now), Cmd.none)
                case Failure(e)    => (newState.copy(error = Some(e.getMessage)), Cmd.none)
            case Failure(e) =>
              (state.copy(downloadStatus = Some(s"❌ Löschen fehlgeschlagen: ${e.getMessage}")), Cmd.none)
        case _             => (state, Cmd.none)

    case BlobViewMsg.RequestUpload =>
      val items = computeFlatItems(state)
      items.lift(state.selectedIndex) match
        case Some(d: Directory) =>
          val blobFolderPath = if d.path.isEmpty then "" else d.path + "/"
          val localItems     = loadLocalItems(".")
          if localItems.isEmpty then (state.copy(downloadStatus = Some("Keine Dateien im aktuellen Verzeichnis")), Cmd.none)
          else
            val uploadState = UploadState(
              containerName = d.containerName,
              blobFolderPath = blobFolderPath,
              localItems = localItems,
              localSelectedIndex = 0,
              localCurrentPath = "."
            )
            (state.copy(pendingUpload = Some(uploadState)), Cmd.none)
        case _                  => (state, Cmd.none)

    case BlobViewMsg.UploadMoveUp =>
      state.pendingUpload.fold((state, Cmd.none)) { upload =>
        val newIdx = moveIndex(upload.localSelectedIndex, -1, upload.localItems.length)
        (state.copy(pendingUpload = Some(upload.copy(localSelectedIndex = newIdx))), Cmd.none)
      }

    case BlobViewMsg.UploadMoveDown =>
      state.pendingUpload.fold((state, Cmd.none)) { upload =>
        val newIdx = moveIndex(upload.localSelectedIndex, 1, upload.localItems.length)
        (state.copy(pendingUpload = Some(upload.copy(localSelectedIndex = newIdx))), Cmd.none)
      }

    case BlobViewMsg.UploadEnter =>
      state.pendingUpload match
        case Some(upload) =>
          upload.localItems.lift(upload.localSelectedIndex) match
            case Some(item: LocalDir)  =>
              val newPath   = if upload.localCurrentPath == "." then item.name else s"${upload.localCurrentPath}/${item.name}"
              val newItems  = loadLocalItems(newPath)
              val newUpload = upload.copy(
                localItems = newItems,
                localSelectedIndex = 0,
                localCurrentPath = newPath
              )
              (state.copy(pendingUpload = Some(newUpload)), Cmd.none)
            case Some(item: LocalFile) =>
              val localFilePath = if upload.localCurrentPath == "." then item.name else s"${upload.localCurrentPath}/${item.name}"
              Try {
                val blobPath = upload.blobFolderPath + item.name
                uploadBlob(upload.containerName, blobPath, localFilePath)(state.storageMode)
              } match
                case Success(path) =>
                  val newState = state.copy(
                    downloadStatus = Some(s"✅ Hochgeladen: $path"),
                    pendingUpload = None
                  )
                  loadContainerData(state.storageMode) match
                    case Success(data) => (newState.copy(containers = data, time = LocalDateTime.now), Cmd.none)
                    case Failure(e)    => (newState.copy(error = Some(e.getMessage)), Cmd.none)
                case Failure(e)    =>
                  (state.copy(downloadStatus = Some(s"❌ Upload fehlgeschlagen: ${e.getMessage}"), pendingUpload = None), Cmd.none)
            case _                     => (state, Cmd.none)
        case None         => (state, Cmd.none)

    case BlobViewMsg.UploadBack =>
      state.pendingUpload match
        case Some(upload) if upload.localCurrentPath != "." =>
          val parentPath = upload.localCurrentPath.lastIndexOf('/') match
            case -1  => "."
            case idx => upload.localCurrentPath.substring(0, idx)
          val newItems   = loadLocalItems(parentPath)
          val newUpload  = upload.copy(
            localItems = newItems,
            localSelectedIndex = 0,
            localCurrentPath = parentPath
          )
          (state.copy(pendingUpload = Some(newUpload)), Cmd.none)
        case _                                              => (state, Cmd.none)

    case BlobViewMsg.CancelUpload =>
      (state.copy(pendingUpload = None), Cmd.none)

  def subscriptions(state: BlobState): Sub[BlobViewMsg] = Sub.batch(
    Sub.time.every(5000, BlobViewMsg.Refresh),
    Sub.onKeyPress {
      case ArrowUpKey   =>
        if state.pendingUpload.isDefined then Some(BlobViewMsg.UploadMoveUp)
        else Some(BlobViewMsg.MoveUp)
      case ArrowDownKey =>
        if state.pendingUpload.isDefined then Some(BlobViewMsg.UploadMoveDown)
        else Some(BlobViewMsg.MoveDown)
      case EnterKey     =>
        if state.pendingUpload.isDefined then Some(BlobViewMsg.UploadEnter)
        else Some(BlobViewMsg.Select)
      case CharKey('a') => Some(BlobViewMsg.Refresh)
      case CharKey('u') => Some(BlobViewMsg.RequestUpload)
      case CharKey('l') => Some(BlobViewMsg.DeleteFile)
      case CharKey('m') => Some(BlobViewMsg.SwitchMode)
      case CharKey('z') =>
        if state.pendingUpload.isDefined then Some(BlobViewMsg.UploadBack)
        else None
      case CharKey('q') => // Quit - zum Abbrechen
        Some(BlobViewMsg.CancelUpload)
      case _            => None
    }
  )

  private def loadContainerData(mode: StorageMode): Try[Map[String, List[BlobInfo]]] = Try {
    val containers = listContainers(mode)
    val futures    = containers.map(name => Future(name -> loadBlobs(name, mode)))
    val results    = Await.result(Future.sequence(futures), 30.seconds)
    results.toMap
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
    Try(downloadBlob(file.containerName, file.blobPath)(state.storageMode)) match
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
      if dir.depth == 0 || expandedPaths.contains(key) then 
        dir :: dir.children.values.toList.sortBy(_.name).flatMap(child => flattenNode(child, expandedPaths))
      else List(dir)

  private def loadLocalItems(path: String): List[LocalItem] =
    import java.io.File
    val dir = new File(path)
    if !dir.exists() || !dir.isDirectory then Nil
    else
      val dirs  = dir.listFiles.filter(_.isDirectory).map(f => LocalDir(f.getName)).toList.sortBy(_.name)
      val files = dir.listFiles.filter(_.isFile).map(f => LocalFile(f.getName)).toList.sortBy(_.name)
      dirs ++ files

  private def moveIndex(current: Int, delta: Int, length: Int): Int =
    if length == 0 then 0
    else (current + delta).max(0).min(length - 1)
