/> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0

import com.azure.identity.*
import com.azure.storage.blob.*

import scala.jdk.CollectionConverters.*

@main
def main(): Unit =

  val CLIENT_ID = "CLIENT_ID"
  val CERTIFICATE_PATH = "CERTIFICATE_PATH"
  val CERTIFICATE_PASSWORD = "PASSWORTHASH"
  val TENANT_ID = "TENANT_ID"
  val STORAGE_ENDPOINT = "https://hubex-auth-t.esma.europa.eu"
  val CONTAINER = "CONTAINER"

  val clientCertificateCredential:ClientCertificateCredential = new ClientCertificateCredentialBuilder()
    .clientId(CLIENT_ID)
      // Choose between either a PEM certificate or a PFX certificate.
      //.pemCertificate(CERTIFICATE_PATH)
      .pfxCertificate(CERTIFICATE_PATH)
      .clientCertificatePassword(CERTIFICATE_PASSWORD) //Use it if the certificate is protected by password
      .tenantId(TENANT_ID)
      .build()

  val blobServiceClient :BlobServiceClient= new BlobServiceClientBuilder()
    .endpoint(STORAGE_ENDPOINT)
    .credential(clientCertificateCredential)
    .buildClient()

  val containerClient :BlobContainerClient= blobServiceClient
    .getBlobContainerClient(CONTAINER)

  val containerPages = containerClient.listBlobs().iterableByPage()

  for containerPage <- containerPages.asScala do
    println(s"Page of blobs received: ${containerPage.getValue.size()}")
    for blobItem <- containerPage.getValue.asScala do
      println(s"\t Blob name: ${blobItem.getName}")

  println("Hello, World!")