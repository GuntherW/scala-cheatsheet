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
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        listDirectory(path) match
          case Left(err)      => ToolResult.error(err)
          case Right(entries) =>
            val output = DirListOutput(
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
            ToolResult.structured(output)

val readFileTool = tool("read_file")
  .description(
    """|Reads the content of a text file on the local filesystem. Returns an error for binary files or files larger than 10 MB.
       |Use offset (1-indexed line number) and limit to read specific sections of large files.""".stripMargin
  )
  .input[ReadFileInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        readFile(path, input.offset, input.limit) match
          case Left(err) => ToolResult.error(err)
          case Right(fc) => ToolResult.text:
              s"""File: ${fc.path}
                 |Size: ${formatSize(fc.sizeBytes)} Lines ${input.offset}-${input.offset + fc.text.linesIterator.length - 1} of ${fc.totalLines}
                 |
                 |${fc.text}""".stripMargin

val searchInFilesTool = tool("search_in_files")
  .description(
    """|Searches for a regex pattern inside files of a directory (recursively).
       |Use fileExtension to filter by extension (e.g. 'scala', 'md', '*' for all).
       |Returns matching lines with file path and line number.""".stripMargin
  )
  .input[SearchInFilesInput]
  .output[SearchResultOutput]
  .handle: input =>
    safePath(input.directory) match
      case Left(err)       => ToolResult.error(err)
      case Right(basePath) =>
        searchInFiles(basePath, input.pattern, input.fileExtension, input.maxResults) match
          case Left(err)    => ToolResult.error(err)
          case Right(found) => ToolResult.structured(SearchResultOutput(
              pattern = input.pattern,
              count = found.size,
              matches = found.map(m => SearchMatchOutput(m.file.toString, m.lineNumber, m.line))
            ))

val fileInfoTool = tool("file_info")
  .description("Returns metadata about a file or directory: size, last modified date, permissions.")
  .input[FileInfoInput]
  .output[FileInfoOutput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        fileInfo(path) match
          case Left(err) => ToolResult.error(err)
          case Right(m)  => ToolResult.structured(FileInfoOutput(
              path = m.path.toString,
              kind = if m.isDirectory then "directory" else "file",
              size = m.sizeBytes.map(formatSize),
              lastModified = m.lastModified,
              readable = m.readable,
              writable = m.writable
            ))

val writeFileTool = tool("write_file")
  .description("Creates or overwrites a file with the given content. Creates parent directories if createDirs is true (default).")
  .input[WriteFileInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) => writeFile(path, input.content, input.createDirs) match
          case Left(err) => ToolResult.error(err)
          case Right(_)  => ToolResult.text(s"File written: $path")

val editFileTool = tool("edit_file")
  .description(
    """|Performs an exact string replacement in a file (oldString → newString).
       |Fails if oldString is not found or matches more than once — provide more surrounding context in that case.""".stripMargin
  )
  .input[EditFileInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) => editFile(path, input.oldString, input.newString) match
          case Left(err) => ToolResult.error(err)
          case Right(_)  => ToolResult.text(s"Edit applied: $path")

val globTool = tool("glob")
  .description(
    """|Fast file pattern matching. Supports glob patterns like "*.scala" or "**/*.ts".
       |Returns matching file paths relative to the given base path.""".stripMargin
  )
  .input[GlobInput]
  .output[GlobOutput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) => globFiles(path, input.pattern) match
          case Left(err)    => ToolResult.error(err)
          case Right(files) => ToolResult.structured(GlobOutput(input.pattern, files.size, files.map(_.toString)))

val createDirectoryTool = tool("create_directory")
  .description("Creates a directory and all its parent directories if they do not exist.")
  .input[CreateDirectoryInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) => createDirectory(path) match
          case Left(err) => ToolResult.error(err)
          case Right(_)  => ToolResult.text(s"Directory created: $path")

val moveTool = tool("move")
  .description("Moves or renames a file or directory. Creates parent directories of the target if createDirs is true (default).")
  .input[MoveInput]
  .handle: input =>
    (safePath(input.from), safePath(input.to)) match
      case (Left(err), _)           => ToolResult.error(err)
      case (_, Left(err))           => ToolResult.error(err)
      case (Right(from), Right(to)) => movePath(from, to, input.createDirs) match
          case Left(err) => ToolResult.error(err)
          case Right(_)  => ToolResult.text(s"Moved: $from → $to")

val copyTool = tool("copy")
  .description("Copies a file or directory to a new location. Creates parent directories of the target if createDirs is true (default).")
  .input[CopyInput]
  .handle: input =>
    (safePath(input.from), safePath(input.to)) match
      case (Left(err), _)           => ToolResult.error(err)
      case (_, Left(err))           => ToolResult.error(err)
      case (Right(from), Right(to)) => copyPath(from, to, input.createDirs) match
          case Left(err) => ToolResult.error(err)
          case Right(_)  => ToolResult.text(s"Copied: $from → $to")

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

// HTTP – für manuelle Nutzung / Tests
@main def filesystemMcpServer(): Unit =
  val endpoint = OxServerHttpTransport(List("mcp")).serve(mcpServer)
  println("Filesystem MCP Server (Ox) starting on http://localhost:8181/mcp")
  NettySyncServer().port(8181).addEndpoint(endpoint).startAndWait()

// stdio – für OpenCode / MCP-Clients die den Prozess selbst starten
@main def filesystemMcpServerStdio(): Unit =
  OxServerStdioTransport().serve(mcpServer)
