//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz:0.6.0
//> using dep com.lihaoyi::os-lib:0.11.9-M7
//> using dep com.softwaremill.ox::core:1.0.4
//> using file BlobService.scala
//> using file Model.scala
//> using file Renderer.scala

import AppMsg.*
import com.azure.storage.blob.BlobServiceClient
import layoutz.*
import ox.*

import java.time.LocalDateTime
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import scala.compiletime.uninitialized
import scala.util.{Failure, Success, Try}

@main
def main(): Unit = BlobViewerApp.run()

object BlobViewerApp extends LayoutzApp[AppState, AppMsg]:

  private var blobClient: BlobServiceClient = uninitialized

  def init: (AppState, Cmd[AppMsg]) =
    val mode = getStorageMode()
    blobClient = createBlobServiceClient(mode)
    loadContainerData(blobClient) match
      case Success(containers) =>
        val expandedPaths = containers.keys.map(c => s"$c:").toSet
        (AppState(containers = containers, time = LocalDateTime.now, storageMode = mode, expandedPaths = expandedPaths), Cmd.none)
      case Failure(e)          => (AppState(time = LocalDateTime.now, error = Some(e.getMessage), storageMode = mode), Cmd.none)

  def view(state: AppState): Element = Renderer.render(state, computeFlatItems)

  def update(msg: AppMsg, state: AppState): (AppState, Cmd[AppMsg]) = msg match

    case LoadError(error) => (state.copy(error = Some(error)), Cmd.none)

    case Refresh =>
      if state.pendingUpload.isDefined || state.pendingZipView.isDefined then (state, Cmd.none)
      else withRefreshedContainers(state.copy(error = None, statusMessage = None))

    case SwitchMode =>
      val newMode = state.storageMode match
        case StorageMode.Azurite   => StorageMode.AzureTest
        case StorageMode.AzureTest => StorageMode.AzureProd
        case StorageMode.AzureProd => StorageMode.Azurite
      blobClient = createBlobServiceClient(newMode)
      withRefreshedContainers(state.copy(storageMode = newMode, statusMessage = Some(StatusMessage.ModeSwitched(newMode))))

    case MoveUp =>
      val newIndex = moveIndex(state.selectedIndex, -1, computeFlatItems(state).length)
      (state.copy(selectedIndex = newIndex), Cmd.none)

    case MoveDown =>
      val newIndex = moveIndex(state.selectedIndex, 1, computeFlatItems(state).length)
      (state.copy(selectedIndex = newIndex), Cmd.none)

    case Select => selectItem(state)

    case DeleteFile =>
      val items = computeFlatItems(state)
      items.lift(state.selectedIndex) match
        case Some(f: FileView) =>
          Try(deleteBlob(blobClient, f.containerName, f.path)) match
            case Success(_) => withRefreshedContainers(state.copy(statusMessage = Some(StatusMessage.Deleted(f.name))))
            case Failure(e) => (state.copy(statusMessage = Some(StatusMessage.DeleteFailed(e.getMessage))), Cmd.none)
        case Some(d: DirView) if d.path.nonEmpty =>
          Try(deleteFolder(blobClient, d.containerName, d.path)) match
            case Success(count) => withRefreshedContainers(state.copy(statusMessage = Some(StatusMessage.Deleted(s"${d.name}/ ($count Dateien)"))))
            case Failure(e)     => (state.copy(statusMessage = Some(StatusMessage.DeleteFailed(e.getMessage))), Cmd.none)
        case _ => (state, Cmd.none)

    case RequestUpload =>
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
              localPath = "."
            )
            (state.copy(pendingUpload = Some(uploadState)), Cmd.none)
        case _                => (state, Cmd.none)

    case UploadMoveUp =>
      state.pendingUpload.fold((state, Cmd.none)) { upload =>
        val newIdx = moveIndex(upload.localSelectedIndex, -1, upload.localItems.length)
        (state.copy(pendingUpload = Some(upload.copy(localSelectedIndex = newIdx))), Cmd.none)
      }

    case UploadMoveDown =>
      state.pendingUpload.fold((state, Cmd.none)) { upload =>
        val newIdx = moveIndex(upload.localSelectedIndex, 1, upload.localItems.length)
        (state.copy(pendingUpload = Some(upload.copy(localSelectedIndex = newIdx))), Cmd.none)
      }

    case UploadEnter =>
      state.pendingUpload match
        case Some(upload) =>
          upload.localItems.lift(upload.localSelectedIndex) match
            case Some(item: DirLocal)  =>
              val newPath   = if upload.localPath == "." then item.name else s"${upload.localPath}/${item.name}"
              val newItems  = loadLocalItems(newPath)
              val newUpload = upload.copy(localItems = newItems, localSelectedIndex = 0, localPath = newPath)
              (state.copy(pendingUpload = Some(newUpload)), Cmd.none)
            case Some(item: FileLocal) =>
              val localFilePath = if upload.localPath == "." then item.name else s"${upload.localPath}/${item.name}"
              Try {
                val blobPath = upload.blobFolderPath + item.name
                uploadBlob(blobClient, upload.containerName, blobPath, localFilePath)
              } match
                case Success(path) => withRefreshedContainers(state.copy(statusMessage = Some(StatusMessage.Uploaded(path)), pendingUpload = None))
                case Failure(e)    => (state.copy(statusMessage = Some(StatusMessage.UploadFailed(e.getMessage)), pendingUpload = None), Cmd.none)
            case _                     => (state, Cmd.none)
        case None         => (state, Cmd.none)

    case CancelUpload => (state.copy(pendingUpload = None), Cmd.none)

    case ViewZip =>
      val items = computeFlatItems(state)
      items.lift(state.selectedIndex) match
        case Some(f: FileView) if f.name.endsWith(".zip") =>
          readZipContents(blobClient, f.containerName, f.path) match
            case scala.util.Success(result) =>
              val xmlContent   = result._1
              val otherEntries = result._2
              val zipState     = ZipViewState(
                containerName = f.containerName,
                blobPath = f.path,
                zipName = f.name,
                xmlContent = xmlContent,
                otherEntries = otherEntries,
                selectedIndex = 0
              )
              (state.copy(pendingZipView = Some(zipState)), Cmd.none)
            case scala.util.Failure(e)      =>
              (state.copy(statusMessage = Some(StatusMessage.ZipViewFailed(e.getMessage))), Cmd.none)
        case Some(f: FileView)                            =>
          (state.copy(statusMessage = Some(StatusMessage.Info(s"Keine ZIP-Datei: ${f.name}"))), Cmd.none)
        case _                                            => (state, Cmd.none)

    case ZipMoveUp =>
      state.pendingZipView.fold((state, Cmd.none)) { zip =>
        val newIdx = moveIndex(zip.selectedIndex, -1, zip.otherEntries.length)
        (state.copy(pendingZipView = Some(zip.copy(selectedIndex = newIdx))), Cmd.none)
      }

    case ZipMoveDown =>
      state.pendingZipView.fold((state, Cmd.none)) { zip =>
        val newIdx = moveIndex(zip.selectedIndex, 1, zip.otherEntries.length)
        (state.copy(pendingZipView = Some(zip.copy(selectedIndex = newIdx))), Cmd.none)
      }

    case ZipScrollUp =>
      state.pendingZipView.fold((state, Cmd.none)) { zip =>
        val newOffset = (zip.scrollOffset - 5).max(0)
        (state.copy(pendingZipView = Some(zip.copy(scrollOffset = newOffset))), Cmd.none)
      }

    case ZipScrollDown =>
      state.pendingZipView.fold((state, Cmd.none)) { zip =>
        val newOffset = zip.scrollOffset + 5
        (state.copy(pendingZipView = Some(zip.copy(scrollOffset = newOffset))), Cmd.none)
      }

    case CloseZipView => (state.copy(pendingZipView = None), Cmd.none)

    case ToggleDirectory =>
      val items = computeFlatItems(state)
      items.lift(state.selectedIndex) match
        case Some(d: DirView) =>
          val key         = d.fullPath
          val newExpanded =
            if state.expandedPaths.contains(key)
            then state.expandedPaths - key
            else state.expandedPaths + key
          (state.copy(expandedPaths = newExpanded), Cmd.none)
        case _                => (state, Cmd.none)

  def subscriptions(state: AppState): Sub[AppMsg] = Sub.batch(
    Sub.time.every(5000, Refresh),
    Sub.onKeyPress {
      case ArrowUpKey   =>
        state.pendingUpload
          .map(_ => UploadMoveUp)
          .orElse(state.pendingZipView.map(_ => ZipScrollUp))
          .orElse(Some(MoveUp))
      case ArrowDownKey =>
        state.pendingUpload
          .map(_ => UploadMoveDown)
          .orElse(state.pendingZipView.map(_ => ZipScrollDown))
          .orElse(Some(MoveDown))
      case EnterKey     =>
        state.pendingUpload
          .map(_ => UploadEnter)
          .orElse(state.pendingZipView.map(_ => CloseZipView))
          .orElse {
            val items = computeFlatItems(state)
            items.lift(state.selectedIndex) match
              case Some(d: DirView)                             => Some(ToggleDirectory)
              case Some(f: FileView) if f.name.endsWith(".zip") => Some(ViewZip)
              case _                                            => None
          }
      case CharKey('a') => Some(Refresh)
      case CharKey('d') => Some(Select) // Download
      case CharKey('u') => Some(RequestUpload)
      case CharKey('l') => Some(DeleteFile)
      case CharKey('m') => Some(SwitchMode)
      case CharKey('q') => state.pendingUpload.map(_ => CancelUpload).orElse(state.pendingZipView.map(_ => CloseZipView)).orElse(Some(CancelUpload))
      case EscapeKey    => state.pendingZipView.map(_ => CloseZipView)
      case _            => None
    }
  )

  private def loadContainerData(client: BlobServiceClient): Try[SortedMap[String, List[BlobInfo]]] = Try {
    val containers = listContainers(client)
    val results    = par(containers.map(name => () => { name -> loadBlobs(client, name) }))

    given Ordering[String] = Ordering.by { name =>
      if name == "esapsdeunr" then (0, "")
      else (1, name.toLowerCase)
    }
    TreeMap.from(results)
  }

  /** Reloads all container data and merges it into the given state. On failure, sets the error field. */
  private def withRefreshedContainers(state: AppState): (AppState, Cmd[AppMsg]) =
    loadContainerData(blobClient) match
      case Success(data) => (state.copy(containers = data, time = LocalDateTime.now), Cmd.none)
      case Failure(e)    => (state.copy(error = Some(e.getMessage)), Cmd.none)

  private def selectItem(state: AppState): (AppState, Cmd[AppMsg]) =
    val items = computeFlatItems(state)
    items.lift(state.selectedIndex) match
      case Some(d: DirView)  => toggleDirectory(state, d)
      case Some(f: FileView) => downloadFile(state, f)
      case None              => (state, Cmd.none)

  private def toggleDirectory(state: AppState, dir: DirView): (AppState, Cmd[AppMsg]) =
    val key         = dir.fullPath
    val newExpanded =
      if state.expandedPaths.contains(key)
      then state.expandedPaths - key
      else state.expandedPaths + key
    (state.copy(expandedPaths = newExpanded), Cmd.none)

  private def downloadFile(state: AppState, file: FileView): (AppState, Cmd[AppMsg]) =
    Try(downloadBlob(blobClient, file.containerName, file.path)) match
      case Success(targetPath) =>
        (state.copy(statusMessage = Some(StatusMessage.Downloaded(file.name, targetPath))), Cmd.none)
      case Failure(e)          =>
        (state.copy(statusMessage = Some(StatusMessage.DownloadFailed(e.getMessage))), Cmd.none)

  private def computeFlatItems(state: AppState): List[NodeView] =
    val items = par(
      state.containers.toList
        .map { (containerName, blobs) => () => (containerName, buildTreeStructure(containerName, blobs)) }
    )
    items.toList
      .flatMap { (_, root) =>
        flattenNode(root, state.expandedPaths)
      }

  private def extractTimestamp(name: String): String =
    val withoutExt = name.lastIndexOf('.') match
      case -1  => name
      case idx => name.substring(0, idx)
    withoutExt.split('_').lastOption.getOrElse("")

  private def flattenNode(node: NodeView, expandedPaths: Set[String]): List[NodeView] = node match
    case file: FileView => List(file)
    case dir: DirView   =>
      if expandedPaths.contains(dir.fullPath)
      then
        dir :: dir.children.values.toList
          .sortWith { (a, b) =>
            (a, b) match
              case (fa: FileView, fb: FileView) => extractTimestamp(fa.name) > extractTimestamp(fb.name)
              case _                            => a.name < b.name
          }
          .take(10)
          .flatMap(child => flattenNode(child, expandedPaths))
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
