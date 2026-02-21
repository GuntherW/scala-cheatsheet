import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}
import layoutz.*

case class AzuriteConfig(accountName: String, accountKey: String, storageEndpoint: String)
case class AzureConfig(blobUrl: String, tenantId: String, clientId: String, pfxBase64: String, pfxPassword: String)

enum StorageMode:
  case Azurite, Azure

sealed trait FileNode:
  def containerName: String
  def name: String
  def depth: Int
case class Directory(containerName: String, path: String, name: String, depth: Int, children: Map[String, FileNode]) extends FileNode
case class File(containerName: String, blobPath: String, name: String, depth: Int)                                   extends FileNode

case class BlobInfo(path: String, name: String)

sealed trait LocalItem:
  def name: String
case class LocalFile(name: String) extends LocalItem
case class LocalDir(name: String)  extends LocalItem

case class BlobState(
    containers: Map[String, List[BlobInfo]] = Map.empty,
    time: LocalDateTime = LocalDateTime.now,
    error: Option[String] = None,
    selectedIndex: Int = 0,
    expandedPaths: Set[String] = Set.empty,
    downloadStatus: Option[String] = None,
    pendingUpload: Option[UploadState] = None,
    storageMode: StorageMode = StorageMode.Azurite
)

case class UploadState(
    containerName: String,
    blobFolderPath: String,
    localItems: List[LocalItem],
    localSelectedIndex: Int,
    localCurrentPath: String
)

enum BlobViewMsg:
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

def pathKey(containerName: String, path: String): String = s"$containerName:$path"
