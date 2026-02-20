//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz::0.6.0
//> using file Model.scala

import com.azure.storage.blob.*
import com.azure.storage.common.StorageSharedKeyCredential
import scala.jdk.CollectionConverters.*
import scala.util.chaining.*

// Azurite Konfiguration (lokaler Azure Storage Emulator)
val config = AzuriteConfig(
  accountName = "devstoreaccount1",
  accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
  storageEndpoint = "http://127.0.0.1:10000/devstoreaccount1"
)

def createBlobServiceClient(): BlobServiceClient = {
  val credential = new StorageSharedKeyCredential(config.accountName, config.accountKey)
  new BlobServiceClientBuilder()
    .endpoint(config.storageEndpoint)
    .credential(credential)
    .buildClient()
}

def listContainers(): List[String] = {
  val blobServiceClient = createBlobServiceClient()
  blobServiceClient
    .listBlobContainers()
    .iterableByPage()
    .asScala
    .flatMap(_.getValue.asScala)
    .map(_.getName)
    .toList
    .sorted
}

def loadBlobs(containerName: String): List[BlobInfo] = {
  val blobServiceClient = createBlobServiceClient()
  val containerClient   = blobServiceClient.getBlobContainerClient(containerName)

  if !containerClient.exists() then Nil
  else
    containerClient
      .listBlobs()
      .iterableByPage()
      .asScala
      .flatMap(_.getValue.asScala)
      .map(blobItem => BlobInfo(blobItem.getName, blobItem.getName.split("/").last))
      .toList
      .sortBy(_.path)
}

def downloadBlob(containerName: String, blobPath: String, targetDir: String = "."): String = {
  import java.nio.file.Paths
  val blobServiceClient = createBlobServiceClient()
  val containerClient   = blobServiceClient.getBlobContainerClient(containerName)
  val blobClient        = containerClient.getBlobClient(blobPath)
  val fileName          = blobPath.split("/").last
  val targetPath        = Paths.get(targetDir, fileName).toAbsolutePath.toString
  blobClient.downloadToFile(targetPath, true)
  targetPath
}

// Erstellt eine Baumstruktur aus den Blob-Pfaden
def buildTreeStructure(containerName: String, blobs: List[BlobInfo]): Directory = {
  def insertPath(root: Directory, parts: List[String], currentPath: String): Directory = parts match {
    case Nil             => root
    case fileName :: Nil =>
      val filePath = if currentPath.isEmpty then fileName else s"$currentPath/$fileName"
      root.copy(children = root.children + (fileName -> File(containerName, filePath, fileName, root.depth + 1)))
    case dirName :: rest =>
      val subPath = if currentPath.isEmpty then dirName else s"$currentPath/$dirName"
      val subDir  = root.children.get(dirName) match
        case Some(dir: Directory) => dir
        case _                    => Directory(containerName, subPath, dirName, root.depth + 1, Map.empty)
      val updated = insertPath(subDir, rest, subPath)
      root.copy(children = root.children + (dirName -> updated))
  }

  blobs.foldLeft(Directory(containerName, "", containerName, 0, Map.empty)) { (root, blob) =>
    insertPath(root, blob.path.split("/").toList, "")
  }
}
