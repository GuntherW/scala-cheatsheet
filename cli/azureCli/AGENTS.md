# AGENTS.md - Azure Blob Viewer CLI

Standalone Scala CLI application for viewing Azure Blob Storage (Azurite) contents in a TUI with interactive navigation and download.

## Project Structure

```
azureCli/
├── MainLocalCli.scala       # Main entry point, UI logic (BlobViewerApp)
├── BlobService.scala        # Azure Blob Storage service functions
├── Model.scala              # Domain types and state
├── .scalafmt.conf           # Formatting config
└── readme.md                # Package instructions
```

## Build Commands

```bash
# Run application
scala-cli run MainLocalCli.scala

# Run with watch mode
scala-cli run MainLocalCli.scala --watch

# Package as executable
scala-cli package MainLocalCli.scala -o az

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
//> using file Model.scala
//> using file BlobService.scala
```

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

case class BlobInfo(path: String, name: String)

sealed trait FileNode:
  def containerName: String
  def name: String
  def depth: Int

case class Directory(containerName: String, path: String, name: String, depth: Int, children: Map[String, FileNode]) extends FileNode
case class File(containerName: String, blobPath: String, name: String, depth: Int)                                   extends FileNode

case class BlobState(
    containers: Map[String, List[BlobInfo]] = Map.empty,
    time: LocalDateTime = LocalDateTime.now,
    error: Option[String] = None,
    selectedIndex: Int = 0,
    expandedPaths: Set[String] = Set.empty,
    downloadStatus: Option[String] = None
)

enum BlobViewMsg:
  case LoadError(error: String)
  case Refresh
  case MoveUp
  case MoveDown
  case Select
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

## Requirements

- Azurite (Azure Storage Emulator) running at `http://127.0.0.1:10000/devstoreaccount1`
