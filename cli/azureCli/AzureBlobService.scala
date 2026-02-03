//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz::0.6.0

import com.azure.storage.blob.*
import com.azure.storage.common.StorageSharedKeyCredential
import scala.jdk.CollectionConverters.*
import scala.util.chaining.*

case class BlobInfo(path: String, name: String)

case class AzureConfig(
    accountName: String,
    accountKey: String,
    storageEndpoint: String
)

// Azurite Konfiguration (lokaler Azure Storage Emulator)
val config = AzureConfig(
  accountName = "devstoreaccount1",
  accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
  storageEndpoint = "http://127.0.0.1:10000/devstoreaccount1"
)

def loadBlobs(containerName: String): List[BlobInfo] = {

  val credential = new StorageSharedKeyCredential(config.accountName, config.accountKey)

  val blobServiceClient = new BlobServiceClientBuilder()
    .endpoint(config.storageEndpoint)
    .credential(credential)
    .buildClient()

  val containerClient = blobServiceClient.getBlobContainerClient(containerName)

  // Container erstellen falls nicht vorhanden
  if !containerClient.exists() then containerClient.create()

  containerClient
    .listBlobs()
    .iterableByPage()
    .asScala
    .flatMap(_.getValue.asScala)
    .map(blobItem => BlobInfo(blobItem.getName, blobItem.getName.split("/").last))
    .toList
    .sortBy(_.path)
}

sealed trait FileNode
case class Directory(name: String, children: Map[String, FileNode]) extends FileNode
case class File(name: String)                                       extends FileNode

// Erstellt eine Baumstruktur aus den Blob-Pfaden
def buildTreeStructure(blobs: List[BlobInfo]): Directory = {
  def insertPath(root: Directory, parts: List[String]): Directory = parts match {
    case Nil             => root
    case fileName :: Nil => root.copy(children = root.children + (fileName -> File(fileName)))
    case dirName :: rest =>
      val subDir        = root.children.get(dirName) match
        case Some(dir: Directory) => dir
        case _                    => Directory(dirName, Map.empty)
      val updatedSubDir = insertPath(subDir, rest)
      root.copy(children = root.children + (dirName -> updatedSubDir))
  }

  blobs.foldLeft(Directory("root", Map.empty)) { (root, blob) =>
    insertPath(root, blob.path.split("/").toList)
  }
}
