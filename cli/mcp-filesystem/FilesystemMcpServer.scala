import chimp.protocol.{GetPromptResult, PromptMessage, ResourceContents, Role, ToolContent}
import chimp.server.*
import chimp.server.ox.{OxServerHttpTransport, OxServerStdioTransport}
import io.circe.Codec
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer

// --- MCP Input-Typen ---
case class ListDirInput(path: String) derives Codec, Schema
case class ReadFileInput(path: String, offset: Int = 1, limit: Option[Int] = None) derives Codec, Schema
case class WriteFileInput(path: String, content: String, createDirs: Boolean = true) derives Codec, Schema
case class EditFileInput(path: String, oldString: String, newString: String) derives Codec, Schema
case class SearchInFilesInput(directory: String, pattern: String, fileExtension: String = "*", maxResults: Int = 50) derives Codec, Schema
case class GlobInput(path: String, pattern: String) derives Codec, Schema
case class FileInfoInput(path: String) derives Codec, Schema
case class CreateDirectoryInput(path: String) derives Codec, Schema
case class MoveInput(from: String, to: String, createDirs: Boolean = true) derives Codec, Schema
case class CopyInput(from: String, to: String, createDirs: Boolean = true) derives Codec, Schema

// --- MCP Output-Typen (strukturierte Rückgaben) ---
case class DirEntryOutput(name: String, kind: String, size: Option[String]) derives Codec, Schema
case class DirListOutput(path: String, count: Int, entries: List[DirEntryOutput]) derives Codec, Schema
case class SearchMatchOutput(file: String, line: Int, text: String) derives Codec, Schema
case class SearchResultOutput(pattern: String, count: Int, matches: List[SearchMatchOutput]) derives Codec, Schema
case class FileInfoOutput(path: String, kind: String, size: Option[String], lastModified: String, readable: Boolean, writable: Boolean) derives Codec, Schema
case class GlobOutput(pattern: String, count: Int, files: List[String]) derives Codec, Schema

// --- MCP Tools ---
val listDirTool = tool("list_directory")
  .description("Lists the contents of a directory on the local filesystem. Returns files and subdirectories with their types and sizes.")
  .input[ListDirInput]
  .output[DirListOutput]
  .handle: input =>
    val result = for
      path    <- safePath(input.path)
      entries <- listDirectory(path)
    yield DirListOutput(
      path = path.toString,
      count = entries.size,
      entries = entries.map { e =>
        DirEntryOutput(
          name = e.name,
          kind = if e.isDirectory then "directory" else "file",
          size = e.sizeBytes.map(formatSize)
        )
      }
    )
    result.fold(ToolResult.error, ToolResult.structured)

val readFileTool = tool("read_file")
  .description(
    """|Reads the content of a text file on the local filesystem. Returns an error for binary files or files larger than 10 MB.
       |Use offset (1-indexed line number) and limit to read specific sections of large files.""".stripMargin
  )
  .input[ReadFileInput]
  .handle: input =>
    val result = for
      path <- safePath(input.path)
      fc   <- readFile(path, input.offset, input.limit)
    yield s"""File: ${fc.path}
              |Size: ${formatSize(fc.sizeBytes)} Lines ${input.offset}-${input.offset + fc.text.linesIterator.length - 1} of ${fc.totalLines}
              |
              |${fc.text}""".stripMargin
    result.fold(ToolResult.error, ToolResult.text)

val searchInFilesTool = tool("search_in_files")
  .description(
    """|Searches for a regex pattern inside files of a directory (recursively).
       |Use fileExtension to filter by extension (e.g. 'scala', 'md', '*' for all).
       |Returns matching lines with file path and line number.""".stripMargin
  )
  .input[SearchInFilesInput]
  .output[SearchResultOutput]
  .handle: input =>
    val result = for
      basePath <- safePath(input.directory)
      found    <- searchInFiles(basePath, input.pattern, input.fileExtension, input.maxResults)
    yield SearchResultOutput(
      pattern = input.pattern,
      count = found.size,
      matches = found.map(m => SearchMatchOutput(m.file.toString, m.lineNumber, m.line))
    )
    result.fold(ToolResult.error, ToolResult.structured)

val fileInfoTool = tool("file_info")
  .description("Returns metadata about a file or directory: size, last modified date, permissions.")
  .input[FileInfoInput]
  .output[FileInfoOutput]
  .handle: input =>
    val result = for
      path <- safePath(input.path)
      m    <- fileInfo(path)
    yield FileInfoOutput(
      path = m.path.toString,
      kind = if m.isDirectory then "directory" else "file",
      size = m.sizeBytes.map(formatSize),
      lastModified = m.lastModified,
      readable = m.readable,
      writable = m.writable
    )
    result.fold(ToolResult.error, ToolResult.structured)

val writeFileTool = tool("write_file")
  .description("Creates or overwrites a file with the given content. Creates parent directories if createDirs is true (default).")
  .input[WriteFileInput]
  .handle: input =>
    val result = for
      path <- safePath(input.path)
      _    <- writeFile(path, input.content, input.createDirs)
    yield s"File written: $path"
    result.fold(ToolResult.error, ToolResult.text)

val editFileTool = tool("edit_file")
  .description(
    """|Performs an exact string replacement in a file (oldString → newString).
       |Fails if oldString is not found or matches more than once — provide more surrounding context in that case.""".stripMargin
  )
  .input[EditFileInput]
  .handle: input =>
    val result = for
      path <- safePath(input.path)
      _    <- editFile(path, input.oldString, input.newString)
    yield s"Edit applied: $path"
    result.fold(ToolResult.error, ToolResult.text)

val globTool = tool("glob")
  .description(
    """|Fast file pattern matching. Supports glob patterns like "*.scala" or "**/*.ts".
       |Returns matching file paths relative to the given base path.""".stripMargin
  )
  .input[GlobInput]
  .output[GlobOutput]
  .handle: input =>
    val result = for
      path  <- safePath(input.path)
      files <- globFiles(path, input.pattern)
    yield GlobOutput(input.pattern, files.size, files.map(_.toString))
    result.fold(ToolResult.error, ToolResult.structured)

val createDirectoryTool = tool("create_directory")
  .description("Creates a directory and all its parent directories if they do not exist.")
  .input[CreateDirectoryInput]
  .handle: input =>
    val result = for
      path <- safePath(input.path)
      _    <- createDirectory(path)
    yield s"Directory created: $path"
    result.fold(ToolResult.error, ToolResult.text)

val moveTool = tool("move")
  .description("Moves or renames a file or directory. Creates parent directories of the target if createDirs is true (default).")
  .input[MoveInput]
  .handle: input =>
    val result = for
      from <- safePath(input.from)
      to   <- safePath(input.to)
      _    <- movePath(from, to, input.createDirs)
    yield s"Moved: $from → $to"
    result.fold(ToolResult.error, ToolResult.text)

val copyTool = tool("copy")
  .description("Copies a file or directory to a new location. Creates parent directories of the target if createDirs is true (default).")
  .input[CopyInput]
  .handle: input =>
    val result = for
      from <- safePath(input.from)
      to   <- safePath(input.to)
      _    <- copyPath(from, to, input.createDirs)
    yield s"Copied: $from → $to"
    result.fold(ToolResult.error, ToolResult.text)

// --- MCP Resources (minimal, nur zur Demonstration) ---

// Feste Resource: liefert immer den Inhalt der README.md dieses Projekts.
val readmeResource = resource("file:///readme")
  .name("README")
  .description("Die README.md dieses MCP-Servers")
  .mimeType("text/markdown")
  .handle: () =>
    readFile(os.pwd / "README.md") match
      case Left(err) => Left(ResourceError(err))
      case Right(fc) => Right(List(ResourceContents.Text(uri = "file:///readme", text = fc.text, mimeType = Some("text/markdown"))))

// Resource-Template: liefert den Inhalt einer Datei im Projekt-Root über eine Namens-Variable in der URI.
// Hinweis: Chimp-URI-Templates matchen pro Variable nur ein Pfadsegment (kein "/"),
// daher hier bewusst auf Dateien im Projekt-Root beschränkt (kein beliebiger Pfad).
val fileResourceTemplate = resourceTemplate("file:///{filename}")
  .name("Datei im Projekt-Root")
  .description("Liest den Inhalt einer Datei im Projekt-Root über ihren Dateinamen")
  .handle: (vars, uri) =>
    val path = os.pwd / vars("filename")
    readFile(path) match
      case Left(err) => Left(ResourceError(err, Some(uri)))
      case Right(fc) => Right(List(ResourceContents.Text(uri = uri, text = fc.text)))

// --- MCP Prompts (minimal, nur zur Demonstration) ---

// Ein Prompt-Template mit einem Pflicht-Argument. Der Client (Nutzer/Host-UI) füllt "path" aus,
// der Server liefert eine fertige Nachrichtenliste, die der Client als User-Prompt an das LLM schickt.
val explainFilePrompt = prompt("explain_file")
  .description("Erzeugt einen Prompt, der das LLM bittet, den Inhalt einer Datei zu erklären.")
  .argument("path", description = Some("Absoluter Pfad zur Datei"), required = true)
  .handle: args =>
    val path = args("path")
    GetPromptResult(
      messages = List(
        PromptMessage(
          role = Role.User,
          content = ToolContent.Text(text = s"Bitte lies die Datei $path und erkläre mir, was der Code darin tut.")
        )
      ),
      description = Some(s"Erklärungs-Prompt für $path")
    )

def mcpServer: StreamingMcpServer[Identity] =
  StreamingMcpServer[Identity]()
    .addTool(listDirTool)
    .addTool(readFileTool)
    .addTool(writeFileTool)
    .addTool(editFileTool)
    .addTool(searchInFilesTool)
    .addTool(globTool)
    .addTool(fileInfoTool)
    .addTool(createDirectoryTool)
    .addTool(moveTool)
    .addTool(copyTool)
    .addResource(readmeResource)
    .addResourceTemplate(fileResourceTemplate)
    .addPrompt(explainFilePrompt)

// HTTP – für manuelle Nutzung / Tests
@main def filesystemMcpServer(): Unit =
  val endpoint = OxServerHttpTransport(List("mcp")).serve(mcpServer)
  println("Filesystem MCP Server (Ox) starting on http://localhost:8181/mcp")
  NettySyncServer().port(8181).addEndpoint(endpoint).startAndWait()

// stdio – für OpenCode / MCP-Clients die den Prozess selbst starten
@main def filesystemMcpServerStdio(): Unit =
  OxServerStdioTransport().serve(mcpServer)
