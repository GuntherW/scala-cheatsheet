# Azure Blob Storage - Lokale Entwicklung mit Azurite

## Azurite starten

```bash
docker-compose up -d
```

## Azurite stoppen

```bash
docker-compose down
```

## Azurite Connection Details

Azurite verwendet feste Credentials für die lokale Entwicklung:

- **Account Name**: `devstoreaccount1`
- **Account Key**: `Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==`
- **Blob Service Endpoint**: `http://127.0.0.1:10000/devstoreaccount1`
- **Connection String**:
  ```
  DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
  ```

## Container erstellen

```bash
# Mit Azure CLI
az storage container create --name CONTAINER --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"

# Oder mit Azure Storage Explorer
# URL: http://127.0.0.1:10000/devstoreaccount1
```

## Code-Anpassungen für Azurite

**Wichtig**: Azurite unterstützt keine Azure AD Authentifizierung (ClientCertificateCredential).
Stattdessen muss Shared Key Authentication oder Connection String verwendet werden.

### Option 1: Mit StorageSharedKeyCredential

```scala
import com.azure.storage.common.StorageSharedKeyCredential

val accountName = "devstoreaccount1"
val accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
val endpoint = "http://127.0.0.1:10000/devstoreaccount1"

val credential = new StorageSharedKeyCredential(accountName, accountKey)

val blobServiceClient = new BlobServiceClientBuilder()
  .endpoint(endpoint)
  .credential(credential)
  .buildClient()
```

### Option 2: Mit Connection String

```scala
val connectionString = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"

val blobServiceClient = new BlobServiceClientBuilder()
  .connectionString(connectionString)
  .buildClient()
```

## Logs anzeigen

```bash
docker-compose logs -f azurite
```

## Daten löschen und neu starten

```bash
docker-compose down -v
docker-compose up -d
```

