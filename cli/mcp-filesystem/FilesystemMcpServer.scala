import chimp.server.*
import chimp.server.ox.OxServerHttpTransport
import io.circe.Codec
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer

// --- MCP Input-Typen ---
case class ListDirInput(path: String) derives Codec, Schema
case class ReadFileInput(path: String) derives Codec, Schema
case class SearchInFilesInput(directory: String, pattern: String, fileExtension: String = "*", maxResults: Int = 50) derives Codec, Schema
case class FileInfoInput(path: String) derives Codec, Schema

// --- MCP Output-Typen (strukturierte Rückgaben) ---
case class DirEntryOutput(name: String, kind: String, size: Option[String]) derives Codec, Schema
case class DirListOutput(path: String, count: Int, entries: List[DirEntryOutput]) derives Codec, Schema
case class SearchMatchOutput(file: String, line: Int, text: String) derives Codec, Schema
case class SearchResultOutput(pattern: String, count: Int, matches: List[SearchMatchOutput]) derives Codec, Schema
case class FileInfoOutput(path: String, kind: String, size: Option[String], lastModified: String, readable: Boolean, writable: Boolean) derives Codec, Schema

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
              entries = entries.map: e =>
                DirEntryOutput(
                  name = e.name,
                  kind = if e.isDirectory then "directory" else "file",
                  size = e.sizeBytes.map(formatSize)
                )
            )
            ToolResult.structured(output)

val readFileTool = tool("read_file")
  .description("Reads the full content of a text file on the local filesystem. Returns an error for binary files or files larger than 1 MB.")
  .input[ReadFileInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        readFile(path) match
          case Left(err) => ToolResult.error(err)
          case Right(fc) =>
            ToolResult.text:
              s"""|File: ${fc.path}
                  |Size: ${formatSize(fc.sizeBytes)}
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
          case Right(found) =>
            val output = SearchResultOutput(
              pattern = input.pattern,
              count = found.size,
              matches = found.map(m => SearchMatchOutput(m.file.toString, m.lineNumber, m.line))
            )
            ToolResult.structured(output)

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
          case Right(m)  =>
            val output = FileInfoOutput(
              path = m.path.toString,
              kind = if m.isDirectory then "directory" else "file",
              size = m.sizeBytes.map(formatSize),
              lastModified = m.lastModified,
              readable = m.readable,
              writable = m.writable
            )
            ToolResult.structured(output)

// --- Server (Ox / direct style) ---
@main def filesystemMcpServer(): Unit =
  val server = StreamingMcpServer[Identity]()
    .addTool(listDirTool)
    .addTool(readFileTool)
    .addTool(searchInFilesTool)
    .addTool(fileInfoTool)

  val endpoint = OxServerHttpTransport(List("mcp")).serve(server)

  println("Filesystem MCP Server (Ox) starting on http://localhost:8181/mcp")
  NettySyncServer().port(8181).addEndpoint(endpoint).startAndWait()
