import java.time.LocalDateTime

case class AzuriteConfig(accountName: String, accountKey: String, storageEndpoint: String)
case class AzureConfig(blobUrl: String, tenantId: String, clientId: String, pfxBase64: String, pfxPassword: String)

enum StorageMode:
  case Azurite, Azure

case class BlobInfo(path: String, name: String, size: Long = 0L)

sealed trait NodeView:
  def containerName: String
  def path: String
  def name: String
  def depth: Int
  def totalSize: Long
case class FileView(containerName: String, path: String, name: String, depth: Int, size: Long = 0L)                extends NodeView:
  def totalSize = size
case class DirView(containerName: String, path: String, name: String, depth: Int, children: Map[String, NodeView]) extends NodeView:
  def fullPath  = s"$containerName:$path"
  def totalSize = children.values.map(_.totalSize).sum

sealed trait ItemLocal:
  def name: String
case class FileLocal(name: String) extends ItemLocal
case class DirLocal(name: String)  extends ItemLocal

enum StatusMessage:
  case Info(message: String)
  case ModeSwitched(mode: StorageMode)
  case Downloaded(name: String, targetPath: String)
  case DownloadFailed(error: String)
  case Uploaded(path: String)
  case UploadFailed(error: String)
  case Deleted(name: String)
  case DeleteFailed(error: String)

  def text: String = this match
    case Info(m)           => m
    case ModeSwitched(m)   => s"🔄 Modus gewechselt zu: $m"
    case Downloaded(n, p)  => s"✅ $n  →  $p"
    case DownloadFailed(e) => s"❌ Download fehlgeschlagen: $e"
    case Uploaded(p)       => s"✅ Hochgeladen: $p"
    case UploadFailed(e)   => s"❌ Upload fehlgeschlagen: $e"
    case Deleted(n)        => s"✅ Gelöscht: $n"
    case DeleteFailed(e)   => s"❌ Löschen fehlgeschlagen: $e"

case class AppState(
    containers: Map[String, List[BlobInfo]] = Map.empty,
    time: LocalDateTime = LocalDateTime.now,
    error: Option[String] = None,
    selectedIndex: Int = 0,
    expandedPaths: Set[String] = Set.empty,
    statusMessage: Option[StatusMessage] = None,
    pendingUpload: Option[UploadState] = None,
    storageMode: StorageMode = StorageMode.Azurite
)

case class UploadState(
    containerName: String,
    blobFolderPath: String,
    localItems: List[ItemLocal],
    localSelectedIndex: Int,
    localPath: String
)

enum AppMsg:
  case LoadError(error: String)
  case Refresh
  case SwitchMode
  case MoveUp
  case MoveDown
  case Select
  case DeleteFile
  case RequestUpload
  case UploadMoveUp
  case UploadMoveDown
  case UploadEnter
  case UploadBack
  case CancelUpload
