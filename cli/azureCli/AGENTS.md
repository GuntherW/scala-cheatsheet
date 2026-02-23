# AGENTS.md - Azure Blob Viewer CLI

Standalone Scala CLI application for viewing Azure Blob Storage (Azurite) contents in a TUI with interactive navigation and download.

## Project Structure

```
azureCli/
├── App.scala                 # Main entry point, UI logic (BlobViewerApp)
├── BlobService.scala        # Azure Blob Storage service functions
├── Model.scala              # Domain types and state
├── .scalafmt.conf           # Formatting config
└── readme.md                # Package instructions
```

## Build Commands

```bash
# Run application
scala-cli run App.scala

# Run with watch mode
scala-cli run App.scala --watch

# Package as executable
scala-cli package App.scala -o az

# Format code with scalafmt
scala-cli scalafmt

# IDE setup
scala-cli setup-ide .
```

## Dependencies

```scala
//> using dep com.azure:azure-sdk-bom:1.3.3
//> using dep com.azure:azure-identity:1.18.1
//> using dep com.azure:azure-storage-blob:12.32.0
//> using dep xyz.matthieucourt::layoutz::0.6.0
//> using dep com.lihaoyi::os-lib:0.11.9-M6
//> using dep com.softwaremill.ox::core:1.0.4
//> using file Model.scala
//> using file BlobService.scala
//> using file Renderer.scala
```
`
Die Grundlogik der Architektur sollte der `layoutz` Bibliothek folgen: https://github.com/mattlianje/layoutz
Für das Durchsuchen des lokalen Verzeichnises wird die Bibiothek https://github.com/com-lihaoyi/os-lib benutzt.
Für Parallelisierung wird die Ox Bibliothek verwendet: https://github.com/softwaremill/ox

## Code Style

- Scala 3, max column: 200
- Wildcard imports: `*` not `_`
- PascalCase: classes, objects, case classes, enums
- camelCase: methods, values
```shell
# Format code with scalafmt
scala-cli scalafmt
```


## Domain Types (Model.scala)

```scala
case class AzuriteConfig(accountName: String, accountKey: String, storageEndpoint: String)
case class AzureConfig(blobUrl: String, tenantId: String, clientId: String, pfxBase64: String, pfxPassword: String)

enum StorageMode:
  case Azurite, Azure

case class BlobInfo(path: String, name: String)

sealed trait NodeView:
  def containerName: String
  def name: String
  def depth: Int

case class FileView(containerName: String, blobPath: String, name: String, depth: Int) extends NodeView:
  def fullPath = s"$containerName:$blobPath"

case class DirView(containerName: String, path: String, name: String, depth: Int, children: Map[String, NodeView]) extends NodeView:
  def fullPath = s"$containerName:$path"

sealed trait ItemLocal:
  def name: String
case class FileLocal(name: String) extends ItemLocal
case class DirLocal(name: String)  extends ItemLocal

enum StatusMessage:
  case Info(message: String)
  case ModeSwitched(mode: StorageMode)
  case Downloaded(name: String, targetPath: String)
  case DownloadFailed(error: String)
  case Uploaded(path: String)
  case UploadFailed(error: String)
  case Deleted(name: String)
  case DeleteFailed(error: String)

  def text: String = this match
    case Info(m)           => m
    case ModeSwitched(m)   => s"🔄 Modus gewechselt zu: $m"
    case Downloaded(n, p)  => s"✅ $n  →  $p"
    case DownloadFailed(e) => s"❌ Download fehlgeschlagen: $e"
    case Uploaded(p)       => s"✅ Hochgeladen: $p"
    case UploadFailed(e)   => s"❌ Upload fehlgeschlagen: $e"
    case Deleted(n)        => s"✅ Gelöscht: $n"
    case DeleteFailed(e)   => s"❌ Löschen fehlgeschlagen: $e"

case class BlobState(
    containers: Map[String, List[BlobInfo]] = Map.empty,
    time: LocalDateTime = LocalDateTime.now,
    error: Option[String] = None,
    selectedIndex: Int = 0,
    expandedPaths: Set[String] = Set.empty,
    statusMessage: Option[StatusMessage] = None,
    pendingUpload: Option[UploadState] = None,
    storageMode: StorageMode = StorageMode.Azurite
)

case class UploadState(
    containerName: String,
    blobFolderPath: String,
    localItems: List[ItemLocal],
    localSelectedIndex: Int,
    localCurrentPath: String
)

enum BlobViewMsg:
  case LoadError(error: String)
  case Refresh
  case SwitchMode
  case MoveUp
  case MoveDown
  case Select
  case DeleteFile
  case RequestUpload
  case UploadMoveUp
  case UploadMoveDown
  case UploadEnter
  case UploadBack
  case CancelUpload

def pathKey(containerName: String, path: String): String = s"$containerName:$path"
```

## Architecture

- **BlobViewerApp**: TUI application using layoutz library
- **BlobState**: Application state with containers, navigation, and download status
- **BlobViewMsg**: Messages for state updates (navigation, refresh, select)
- **listContainers()**: Dynamically loads all containers from Azurite
- **loadBlobs()**: Fetches blobs from a container
- **downloadBlob()**: Downloads a blob to local filesystem
- **buildTreeStructure()**: Converts flat blob list to tree structure

## Features

- Dynamic container discovery (no hardcoded containers)
- Interactive tree navigation (↑↓ arrows, Enter to expand/download)
- Automatic refresh every 5 seconds
- Blob download on Enter (files only)
- Tree view with depth-based indentation
- File count per folder displayed
- Mode switching between Azurite and Azure (key: m)
- Upload files to blob storage (key: u)
- Delete files (key: l)
- Refresh (key: a)

## Requirements

- Azurite (Azure Storage Emulator) running at `http://127.0.0.1:10000/devstoreaccount1`
