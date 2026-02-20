import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}
import layoutz.*

case class AzuriteConfig(
    accountName: String,
    accountKey: String,
    storageEndpoint: String
)

sealed trait FileNode:
  def containerName: String
  def name: String
  def depth: Int

case class Directory(containerName: String, path: String, name: String, depth: Int, children: Map[String, FileNode]) extends FileNode
case class File(containerName: String, blobPath: String, name: String, depth: Int)                                   extends FileNode

case class BlobInfo(path: String, name: String)
case class BlobState(
    containers: Map[String, List[BlobInfo]] = Map.empty,
    time: LocalDateTime = LocalDateTime.now,
    error: Option[String] = None,
    selectedIndex: Int = 0,
    expandedPaths: Set[String] = Set.empty, // "containerName:path" – leerer Pfad = Container-Root
    downloadStatus: Option[String] = None
)

enum BlobViewMsg:
  case LoadError(error: String)
  case Refresh
  case MoveUp
  case MoveDown
  case Select
