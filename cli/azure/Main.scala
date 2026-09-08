//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0

import com.azure.identity.*
import com.azure.storage.blob.*

import scala.jdk.CollectionConverters.*

@main
def main(): Unit =

  val clientId            = sys.env.getOrElse("AZURE_CLIENT_ID", sys.error("AZURE_CLIENT_ID not set"))
  val certificatePath     = sys.env.getOrElse("AZURE_CERTIFICATE_PATH", sys.error("AZURE_CERTIFICATE_PATH not set"))
  val certificatePassword = sys.env.getOrElse("AZURE_CERTIFICATE_PASSWORD", sys.error("AZURE_CERTIFICATE_PASSWORD not set"))
  val tenantId            = sys.env.getOrElse("AZURE_TENANT_ID", sys.error("AZURE_TENANT_ID not set"))
  val storageEndpoint     = sys.env.getOrElse("AZURE_STORAGE_ENDPOINT", sys.error("AZURE_STORAGE_ENDPOINT not set"))
  val container           = sys.env.getOrElse("AZURE_CONTAINER", sys.error("AZURE_CONTAINER not set"))

  val clientCertificateCredential: ClientCertificateCredential = new ClientCertificateCredentialBuilder()
    .clientId(clientId)
    // Choose between either a PEM certificate or a PFX certificate.
    // .pemCertificate(certificatePath)
    .pfxCertificate(certificatePath)
    .clientCertificatePassword(certificatePassword) // Use it if the certificate is protected by password
    .tenantId(tenantId)
    .build()

  val blobServiceClient: BlobServiceClient = new BlobServiceClientBuilder()
    .endpoint(storageEndpoint)
    .credential(clientCertificateCredential)
    .buildClient()

  val containerClient: BlobContainerClient = blobServiceClient
    .getBlobContainerClient(container)

  val containerPages = containerClient.listBlobs().iterableByPage()

  for containerPage <- containerPages.asScala do
    println(s"Page of blobs received: ${containerPage.getValue.size()}")
    for blobItem <- containerPage.getValue.asScala do
      println(s"\t Blob name: ${blobItem.getName}")

  println("Hello, World!")
