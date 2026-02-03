//> using dep com.azure:azure-sdk-bom:1.3.4
//> using dep com.azure:azure-identity:1.18.2
//> using dep com.azure:azure-storage-blob:12.33.1

import com.azure.identity.*
import com.azure.storage.blob.*
import com.azure.storage.common.StorageSharedKeyCredential

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import scala.jdk.CollectionConverters.*

@main
def mainLocal(): Unit =

  // Azurite standard credentials
  val accountName = "devstoreaccount1"
  val accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
  val storageEndpoint = "http://127.0.0.1:10000/devstoreaccount1"
  val containerName = "test-container"

  // Create shared key credential for Azurite
  val credential = new StorageSharedKeyCredential(accountName, accountKey)

  val blobServiceClient: BlobServiceClient = new BlobServiceClientBuilder()
    .endpoint(storageEndpoint)
    .credential(credential)
    .buildClient()

  val containerClient: BlobContainerClient = blobServiceClient.getBlobContainerClient(containerName)

  // Create container if it doesn't exist
  if !containerClient.exists() then
    println(s"Creating container: $containerName")
    containerClient.create()
  else
    println(s"Container exists: $containerName")



  // Upload files to a virtual directory
  val directory = "documents/reports"
  val content = "Hello from Azurite! This is a test file in a virtual directory."

  uploadFile(containerClient, s"$directory/test-file1.txt", content)
  uploadFile(containerClient, s"$directory/test-file2.txt", content)
  uploadFile(containerClient, "archive/old-data.txt", "Archived data")


  // List all blobs in the container
  println(s"\nListing all blobs in container '$containerName':")
  val containerPages = containerClient.listBlobs().iterableByPage()

  for containerPage <- containerPages.asScala do
    println(s"\nPage of blobs received: ${containerPage.getValue.size()}")
    for blobItem <- containerPage.getValue.asScala do
      println(s"  📄 ${blobItem.getName}")

  // Download and display an uploaded file
  val testPath = s"$directory/test-file1.txt"
  println(s"\nDownloading and reading file: $testPath")
  val blobClientForDownload = containerClient.getBlobClient(testPath) // Need BlobClient for this specific path
  val downloadStream = new ByteArrayOutputStream()
  blobClientForDownload.downloadStream(downloadStream)
  val downloadedContent = downloadStream.toString("UTF-8")
  println(s"✓ Downloaded content:\n  '$downloadedContent'")

  println("✅ All operations completed successfully!")

// Helper function to upload a file
// Note: BlobClient is path-specific - each file path needs its own BlobClient
def uploadFile(containerClient: BlobContainerClient, blobPath: String, content: String): Unit =
  val blobClient = containerClient.getBlobClient(blobPath) // One BlobClient per file path
  val inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"))
  blobClient.upload(inputStream, content.length, true) // true = overwrite if exists
  println(s"✓ File uploaded successfully: $blobPath")
