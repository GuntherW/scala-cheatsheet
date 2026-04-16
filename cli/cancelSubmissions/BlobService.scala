import com.azure.identity.ClientCertificateCredentialBuilder
import com.azure.storage.blob.*
import com.azure.storage.common.StorageSharedKeyCredential

import java.util.Base64
import scala.jdk.CollectionConverters.*

// ─── Konfigurationen ──────────────────────────────────────────────────────────

case class AzuriteConfig(accountName: String, accountKey: String, storageEndpoint: String)
case class AzureConfig(blobUrl: String, tenantId: String, clientId: String, pfxBase64: String, pfxPassword: String)
case class AzureProdConfig(blobUrl: String, tenantId: String, clientId: String, pfxBase64: String, pfxPassword: String)

enum StorageMode:
  case Azurite, AzureTest, AzureProd

case class BlobInfo(path: String, name: String, size: Long = 0L)

// ─── Hardcoded Azurite-Credentials ───────────────────────────────────────────

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

// ─── Client-Erstellung ───────────────────────────────────────────────────────

def createBlobServiceClient(mode: StorageMode): BlobServiceClient = mode match
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

// ─── Blob-Operationen ─────────────────────────────────────────────────────────

val containerName = "esapsdeunr"

/** Listet alle Blobs unter einem bestimmten Präfix im Container. */
def listBlobsWithPrefix(client: BlobServiceClient, prefix: String): List[BlobInfo] =
  val containerClient = client.getBlobContainerClient(containerName)
  if !containerClient.exists() then Nil
  else
    containerClient
      .listBlobs()
      .iterableByPage()
      .asScala
      .flatMap(_.getValue.asScala)
      .filter(_.getName.startsWith(prefix))
      .map(b => BlobInfo(b.getName, b.getName.split("/").last, b.getProperties.getContentLength))
      .toList
      .sortBy(_.path)

/** Lädt einen Blob herunter und speichert ihn im Zielverzeichnis. Gibt den lokalen Pfad zurück. */
def downloadBlob(client: BlobServiceClient, blobPath: String, targetDir: String): String =
  import java.nio.file.Paths
  val containerClient = client.getBlobContainerClient(containerName)
  val blobClient      = containerClient.getBlobClient(blobPath)
  val fileName        = blobPath.split("/").last
  val targetPath      = Paths.get(targetDir, fileName).toAbsolutePath.toString
  blobClient.downloadToFile(targetPath, true)
  targetPath

/** Lädt eine lokale Datei in den angegebenen Blob-Pfad hoch. */
def uploadBlob(client: BlobServiceClient, blobPath: String, localFilePath: String): String =
  val containerClient = client.getBlobContainerClient(containerName)
  val blobClient      = containerClient.getBlobClient(blobPath)
  blobClient.uploadFromFile(localFilePath, true)
  blobPath
