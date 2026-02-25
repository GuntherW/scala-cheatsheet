//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz::0.6.0
//> using dep com.lihaoyi::os-lib:0.11.9-M6
//> using file Model.scala

import com.azure.identity.ClientCertificateCredentialBuilder
import com.azure.storage.blob.*
import com.azure.storage.common.StorageSharedKeyCredential

import java.util.Base64
import scala.jdk.CollectionConverters.*

// Azurite Konfiguration (lokaler Azure Storage Emulator)
lazy val azuriteConfig = AzuriteConfig(
  accountName = "devstoreaccount1",
  accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
  storageEndpoint = "http://127.0.0.1:10000/devstoreaccount1"
)

lazy val azureConfig: AzureConfig =
  AzureConfig(
    blobUrl = sys.env.getOrElse("AZURE_BLOB_URL", sys.error("AZURE_BLOB_URL not set")),
    tenantId = sys.env.getOrElse("AZURE_TENANT_ID", sys.error("AZURE_TENANT_ID not set")),
    clientId = sys.env.getOrElse("AZURE_CLIENT_ID", sys.error("AZURE_CLIENT_ID not set")),
    pfxBase64 = sys.env.getOrElse("ESAP_PFX_FILE_BASE64", sys.error("ESAP_PFX_FILE_BASE64 not set")),
    pfxPassword = sys.env.getOrElse("ESAP_HUBEX_PASSWORD", sys.error("ESAP_HUBEX_PASSWORD not set"))
  )

def getStorageMode(): StorageMode =
  sys.env.get("STORAGE_MODE").map(_.toLowerCase) match
    case Some("azure") => StorageMode.Azure
    case _             => StorageMode.Azurite

def createBlobServiceClient(mode: StorageMode = getStorageMode()): BlobServiceClient = mode match
  case StorageMode.Azurite =>
    val credential = new StorageSharedKeyCredential(azuriteConfig.accountName, azuriteConfig.accountKey)
    new BlobServiceClientBuilder()
      .endpoint(azuriteConfig.storageEndpoint)
      .credential(credential)
      .buildClient()
  case StorageMode.Azure   =>
    val pfxBytes = Base64.getDecoder().decode(azureConfig.pfxBase64)

    val credential = new ClientCertificateCredentialBuilder()
      .tenantId(azureConfig.tenantId)
      .clientId(azureConfig.clientId)
      .pfxCertificate(new java.io.ByteArrayInputStream(pfxBytes))
      .clientCertificatePassword(azureConfig.pfxPassword)
      .build()

    new BlobServiceClientBuilder()
      .endpoint(azureConfig.blobUrl)
      .credential(credential)
      .buildClient()

def listContainers(client: BlobServiceClient): List[String] = {
  client
    .listBlobContainers()
    .iterableByPage()
    .asScala
    .flatMap(_.getValue.asScala)
    .map(_.getName)
    .toList
    .sortBy { name => if name == "esapsdeunr" then (0, name) else (1, name) }
    .filter(_.startsWith("esapsdeunr")) // temporär. Nur "unsere" Container anzeigen.
}

def loadBlobs(client: BlobServiceClient, containerName: String): List[BlobInfo] = {
  val containerClient = client.getBlobContainerClient(containerName)

  if !containerClient.exists() then Nil
  else
    containerClient
      .listBlobs()
      .iterableByPage()
      .asScala
      .flatMap(_.getValue.asScala)
      .map(blobItem => BlobInfo(blobItem.getName, blobItem.getName.split("/").last, blobItem.getProperties.getContentLength))
      .toList
      .sortBy(_.path)
}

def downloadBlob(client: BlobServiceClient, containerName: String, blobPath: String, targetDir: String = "."): String = {
  import java.nio.file.Paths
  val containerClient = client.getBlobContainerClient(containerName)
  val blobClient      = containerClient.getBlobClient(blobPath)
  val fileName        = blobPath.split("/").last
  val targetPath      = Paths.get(targetDir, fileName).toAbsolutePath.toString
  blobClient.downloadToFile(targetPath, true)
  targetPath
}

def uploadBlob(client: BlobServiceClient, containerName: String, blobPath: String, localFilePath: String): String = {
  val containerClient = client.getBlobContainerClient(containerName)
  val blobClient      = containerClient.getBlobClient(blobPath)
  blobClient.uploadFromFile(localFilePath, true)
  blobPath
}

def deleteBlob(client: BlobServiceClient, containerName: String, blobPath: String): String = {
  val containerClient = client.getBlobContainerClient(containerName)
  val blobClient      = containerClient.getBlobClient(blobPath)
  blobClient.delete()
  blobPath
}

// Erstellt eine Baumstruktur aus den Blob-Pfaden
def buildTreeStructure(containerName: String, blobs: List[BlobInfo]): DirView = {
  val knownDirs = Set("incoming", "outgoing", "forward", "archive", "public")

  def insertPath(root: DirView, parts: List[String], currentPath: String, blobSize: Long): DirView = parts match {
    case Nil             => root
    case fileName :: Nil =>
      val filePath = if currentPath.isEmpty then fileName else s"$currentPath/$fileName"
      if knownDirs.contains(fileName) then
        val dir = root.children.get(fileName) match
          case Some(d: DirView) => d
          case _                => DirView(containerName, filePath, fileName, root.depth + 1, Map.empty)
        root.copy(children = root.children + (fileName -> dir))
      else root.copy(children = root.children + (fileName -> FileView(containerName, filePath, fileName, root.depth + 1, blobSize)))
    case dirName :: rest =>
      val subPath = if currentPath.isEmpty then dirName else s"$currentPath/$dirName"
      val subDir  = root.children.get(dirName) match
        case Some(dir: DirView) => dir
        case _                  => DirView(containerName, subPath, dirName, root.depth + 1, Map.empty)
      val updated = insertPath(subDir, rest, subPath, blobSize)
      root.copy(children = root.children + (dirName -> updated))
  }

  val tree = blobs.foldLeft(DirView(containerName, "", containerName, 0, Map.empty)) { (root, blob) =>
    insertPath(root, blob.path.split("/").toList, "", blob.size)
  }
  tree
}
