//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz::0.6.0
//> using dep com.lihaoyi::os-lib:0.11.9-M7
//> using file Model.scala

import com.azure.identity.ClientCertificateCredentialBuilder
import com.azure.storage.blob.*
import com.azure.storage.blob.models.ListBlobsOptions
import com.azure.storage.common.StorageSharedKeyCredential

import java.util.Base64
import java.util.zip.ZipFile
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.dom.DOMSource
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

import java.io.{StringReader, StringWriter}
import scala.jdk.CollectionConverters.*
import scala.util.Try

// Azurite Konfiguration (lokaler Azure Storage Emulator)
lazy val azuriteConfig = AzuriteConfig(
  accountName = "devstoreaccount1",
  accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
  storageEndpoint = "http://127.0.0.1:10000/devstoreaccount1"
)

lazy val azureTestConfig: AzureConfig =
  AzureConfig(
    blobUrl = sys.env.getOrElse("AZURE_BLOB_URL_TEST", sys.error("AZURE_BLOB_URL_TEST not set")),
    tenantId = sys.env.getOrElse("AZURE_TENANT_ID_TEST", sys.error("AZURE_TENANT_ID_TEST not set")),
    clientId = sys.env.getOrElse("AZURE_CLIENT_ID_TEST", sys.error("AZURE_CLIENT_ID_TEST not set")),
    pfxBase64 = sys.env.getOrElse("ESAP_PFX_FILE_BASE64_TEST", sys.error("ESAP_PFX_FILE_BASE64_TEST not set")),
    pfxPassword = sys.env.getOrElse("ESAP_HUBEX_PASSWORD_TEST", sys.error("ESAP_HUBEX_PASSWORD_TEST not set"))
  )

lazy val azureProdConfig: AzureProdConfig =
  AzureProdConfig(
    blobUrl = sys.env.getOrElse("AZURE_BLOB_URL_PROD", sys.error("AZURE_BLOB_URL_PROD not set")),
    tenantId = sys.env.getOrElse("AZURE_TENANT_ID_PROD", sys.error("AZURE_TENANT_ID_PROD not set")),
    clientId = sys.env.getOrElse("AZURE_CLIENT_ID_PROD", sys.error("AZURE_CLIENT_ID_PROD not set")),
    pfxBase64 = sys.env.getOrElse("ESAP_PFX_FILE_BASE64_PROD", sys.error("ESAP_PFX_FILE_BASE64_PROD not set")),
    pfxPassword = sys.env.getOrElse("ESAP_HUBEX_PASSWORD_PROD", sys.error("ESAP_HUBEX_PASSWORD_PROD not set"))
  )

def getStorageMode(): StorageMode =
  sys.env.get("STORAGE_MODE").map(_.toLowerCase) match
    case Some("azuretest") => StorageMode.AzureTest
    case Some("azureprod") => StorageMode.AzureProd
    case Some("azure")     => StorageMode.AzureTest
    case _                 => StorageMode.Azurite

def createBlobServiceClient(mode: StorageMode = getStorageMode()): BlobServiceClient = mode match
  case StorageMode.Azurite   =>
    val credential = new StorageSharedKeyCredential(azuriteConfig.accountName, azuriteConfig.accountKey)
    new BlobServiceClientBuilder()
      .endpoint(azuriteConfig.storageEndpoint)
      .credential(credential)
      .buildClient()
  case StorageMode.AzureTest =>
    val credential = new ClientCertificateCredentialBuilder()
      .tenantId(azureTestConfig.tenantId)
      .clientId(azureTestConfig.clientId)
      .pfxCertificate(new java.io.ByteArrayInputStream(Base64.getDecoder.decode(azureTestConfig.pfxBase64)))
      .clientCertificatePassword(azureTestConfig.pfxPassword)
      .build()

    new BlobServiceClientBuilder()
      .endpoint(azureTestConfig.blobUrl)
      .credential(credential)
      .buildClient()
  case StorageMode.AzureProd =>
    val credential = new ClientCertificateCredentialBuilder()
      .tenantId(azureProdConfig.tenantId)
      .clientId(azureProdConfig.clientId)
      .pfxCertificate(new java.io.ByteArrayInputStream(Base64.getDecoder.decode(azureProdConfig.pfxBase64)))
      .clientCertificatePassword(azureProdConfig.pfxPassword)
      .build()

    new BlobServiceClientBuilder()
      .endpoint(azureProdConfig.blobUrl)
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

def deleteFolder(client: BlobServiceClient, containerName: String, folderPath: String): Int = {
  val containerClient = client.getBlobContainerClient(containerName)
  val prefix          = folderPath + "/"
  val blobs           = containerClient.listBlobs().asScala.toList.filter(_.getName.startsWith(prefix))
  blobs.foreach { blobItem =>
    containerClient.getBlobClient(blobItem.getName).delete()
  }
  blobs.length
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

def readZipContents(client: BlobServiceClient, containerName: String, blobPath: String): Try[(String, List[ZipEntryInfo])] = Try {
  val tempDir  = os.Path(java.io.File.createTempFile("temp", ".tmp").getParentFile)
  val fileName = blobPath.split("/").last
  val tempFile = tempDir / fileName

  try
    val containerClient = client.getBlobContainerClient(containerName)
    val blobClient      = containerClient.getBlobClient(blobPath)
    blobClient.downloadToFile(tempFile.toString, true)

    val zipFile    = new ZipFile(tempFile.toString)
    val zipEntries = zipFile.entries().asScala.toList
    val baseName   = fileName.stripSuffix(".zip")

    def stripTimestamp(name: String): String =
      val lastUnderscore = name.lastIndexOf('_')
      if lastUnderscore > 0 && lastUnderscore < name.length - 1 && name.substring(lastUnderscore + 1).forall(_.isDigit)
      then name.substring(0, lastUnderscore)
      else name

    val baseNameNoTimestamp = stripTimestamp(baseName)
    val matchingXmlExact    = s"$baseName.xml"
    val matchingXmlNoTs     = s"$baseNameNoTimestamp.xml"

    val xmlEntry = zipEntries.find(e =>
      e.getName == matchingXmlExact || e.getName == matchingXmlNoTs ||
        e.getName.endsWith(".xml") && (e.getName.startsWith(baseName) || e.getName.startsWith(baseNameNoTimestamp))
    )

    val xmlContent = xmlEntry match
      case Some(entry) =>
        val stream = zipFile.getInputStream(entry)
        val bytes  = stream.readNBytes(Integer.MAX_VALUE)
        stream.close()
        new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
      case None        => ""

    val otherEntries = zipEntries
      .filter(e =>
        !e.isDirectory &&
          e.getName != matchingXmlExact &&
          e.getName != matchingXmlNoTs &&
          !e.getName.startsWith(baseName) &&
          !e.getName.startsWith(baseNameNoTimestamp)
      )
      .map(e => ZipEntryInfo(e.getName, e.getName.endsWith(".xml"), false))
      .sortBy(_.name)

    zipFile.close()
    (formatXml(xmlContent), otherEntries)
  finally if os.exists(tempFile) then os.remove(tempFile)
}

def formatXml(xml: String): String =
  if xml.isEmpty then ""
  else
    try
      val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
      val doc     = builder.parse(new org.xml.sax.InputSource(new StringReader(xml)))
      doc.getDocumentElement.normalize()

      val transformer = TransformerFactory.newInstance().newTransformer()
      transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
      transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes")

      val writer = new StringWriter()
      transformer.transform(new DOMSource(doc), new StreamResult(writer))
      writer.toString
    catch case _: Exception => xml
