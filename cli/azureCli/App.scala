//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz:0.6.0
//> using dep com.lihaoyi::os-lib:0.11.9-M6
//> using dep com.softwaremill.ox::core:1.0.4
//> using file BlobService.scala
//> using file Model.scala
//> using file Renderer.scala

import com.azure.storage.blob.BlobServiceClient
import layoutz.*
import ox.*

import java.time.LocalDateTime
import scala.compiletime.uninitialized
import scala.util.{Failure, Success, Try}

@main
def main(): Unit = BlobViewerApp.run()

object BlobViewerApp extends LayoutzApp[BlobState, BlobViewMsg]:

  private var blobClient: BlobServiceClient = uninitialized

  def init: (BlobState, Cmd[BlobViewMsg]) =
    val mode = getStorageMode()
    blobClient = createBlobServiceClient(mode)
    loadContainerData(blobClient) match
      case Success(data) => (BlobState(containers = data, time = LocalDateTime.now, storageMode = mode), Cmd.none)
      case Failure(e)    => (BlobState(time = LocalDateTime.now, error = Some(e.getMessage), storageMode = mode), Cmd.none)

  def view(state: BlobState): Element =
    Renderer.render(state, computeFlatItems)

  def update(msg: BlobViewMsg, state: BlobState): (BlobState, Cmd[BlobViewMsg]) = msg match

    case BlobViewMsg.LoadError(error) => (state.copy(error = Some(error)), Cmd.none)

    case BlobViewMsg.Refresh =>
      if state.pendingUpload.isDefined then (state, Cmd.none)
      else withRefreshedContainers(state.copy(error = None, statusMessage = None))

    case BlobViewMsg.SwitchMode =>
      val newMode = state.storageMode match
        case StorageMode.Azurite => StorageMode.Azure
        case StorageMode.Azure   => StorageMode.Azurite
      blobClient = createBlobServiceClient(newMode)
      withRefreshedContainers(state.copy(storageMode = newMode, statusMessage = Some(StatusMessage.ModeSwitched(newMode))))

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
        case Some(f: FileView) =>
          Try(deleteBlob(blobClient, f.containerName, f.path)) match
            case Success(_) => withRefreshedContainers(state.copy(statusMessage = Some(StatusMessage.Deleted(f.name))))
            case Failure(e) => (state.copy(statusMessage = Some(StatusMessage.DeleteFailed(e.getMessage))), Cmd.none)
        case _                 => (state, Cmd.none)

    case BlobViewMsg.RequestUpload =>
      val items = computeFlatItems(state)
      items.lift(state.selectedIndex) match
        case Some(d: DirView) =>
          val blobFolderPath = if d.path.isEmpty then "" else d.path + "/"
          val localItems     = loadLocalItems(".")
          if localItems.isEmpty then (state.copy(statusMessage = Some(StatusMessage.Info("Keine Dateien im aktuellen Verzeichnis"))), Cmd.none)
          else
            val uploadState = UploadState(
              containerName = d.containerName,
              blobFolderPath = blobFolderPath,
              localItems = localItems,
              localSelectedIndex = 0,
              localCurrentPath = "."
            )
            (state.copy(pendingUpload = Some(uploadState)), Cmd.none)
        case _                => (state, Cmd.none)

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
            case Some(item: DirLocal)  =>
              val newPath   = if upload.localCurrentPath == "." then item.name else s"${upload.localCurrentPath}/${item.name}"
              val newItems  = loadLocalItems(newPath)
              val newUpload = upload.copy(
                localItems = newItems,
                localSelectedIndex = 0,
                localCurrentPath = newPath
              )
              (state.copy(pendingUpload = Some(newUpload)), Cmd.none)
            case Some(item: FileLocal) =>
              val localFilePath = if upload.localCurrentPath == "." then item.name else s"${upload.localCurrentPath}/${item.name}"
              Try {
                val blobPath = upload.blobFolderPath + item.name
                uploadBlob(blobClient, upload.containerName, blobPath, localFilePath)
              } match
                case Success(path) =>
                  withRefreshedContainers(state.copy(statusMessage = Some(StatusMessage.Uploaded(path)), pendingUpload = None))
                case Failure(e)    =>
                  (state.copy(statusMessage = Some(StatusMessage.UploadFailed(e.getMessage)), pendingUpload = None), Cmd.none)
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

  private def loadContainerData(client: BlobServiceClient): Try[Map[String, List[BlobInfo]]] = Try {
    val containers = listContainers(client)
    val results    = par(containers.map(name => () => { name -> loadBlobs(client, name) }))
    results.toMap
  }

  /** Reloads all container data and merges it into the given state. On failure, sets the error field. */
  private def withRefreshedContainers(state: BlobState): (BlobState, Cmd[BlobViewMsg]) =
    loadContainerData(blobClient) match
      case Success(data) => (state.copy(containers = data, time = LocalDateTime.now), Cmd.none)
      case Failure(e)    => (state.copy(error = Some(e.getMessage)), Cmd.none)

  private def selectItem(state: BlobState): (BlobState, Cmd[BlobViewMsg]) =
    val items = computeFlatItems(state)
    items.lift(state.selectedIndex) match
      case Some(d: DirView)  => toggleDirectory(state, d)
      case Some(f: FileView) => downloadFile(state, f)
      case None              => (state, Cmd.none)

  private def toggleDirectory(state: BlobState, dir: DirView): (BlobState, Cmd[BlobViewMsg]) =
    val key         = dir.fullPath
    val newExpanded =
      if state.expandedPaths.contains(key)
      then state.expandedPaths - key
      else state.expandedPaths + key
    (state.copy(expandedPaths = newExpanded), Cmd.none)

  private def downloadFile(state: BlobState, file: FileView): (BlobState, Cmd[BlobViewMsg]) =
    Try(downloadBlob(blobClient, file.containerName, file.path)) match
      case Success(targetPath) =>
        (state.copy(statusMessage = Some(StatusMessage.Downloaded(file.name, targetPath))), Cmd.none)
      case Failure(e)          =>
        (state.copy(statusMessage = Some(StatusMessage.DownloadFailed(e.getMessage))), Cmd.none)

  private def computeFlatItems(state: BlobState): List[NodeView] =
    val items = par(
      state.containers.toList.sortBy(_._1).map { (containerName, blobs) => () => (containerName, buildTreeStructure(containerName, blobs)) }
    )
    items.toList.sortBy(_._1).flatMap { (_, root) =>
      flattenNode(root, state.expandedPaths)
    }

  private def flattenNode(node: NodeView, expandedPaths: Set[String]): List[NodeView] = node match
    case file: FileView => List(file)
    case dir: DirView   =>
      val key = dir.fullPath
      if dir.depth == 0 || expandedPaths.contains(key) then dir :: dir.children.values.toList.sortBy(_.name).flatMap(child => flattenNode(child, expandedPaths))
      else List(dir)

  private def loadLocalItems(path: String): List[ItemLocal] =
    val dir = os.Path(path, os.pwd)
    if !os.exists(dir) || !os.isDir(dir) then Nil
    else
      val entries = os.list(dir)
      val dirs    = entries.filter(os.isDir(_)).map(p => DirLocal(p.last)).toList.sortBy(_.name)
      val files   = entries.filter(os.isFile(_)).map(p => FileLocal(p.last)).toList.sortBy(_.name)
      dirs ++ files

  private def moveIndex(current: Int, delta: Int, length: Int): Int =
    if length == 0 then 0
    else (current + delta).max(0).min(length - 1)
