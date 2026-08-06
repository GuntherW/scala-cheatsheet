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

// --- MCP Tools ---

val listDirTool = tool("list_directory")
  .description("Lists the contents of a directory on the local filesystem. Returns files and subdirectories with their types and sizes.")
  .input[ListDirInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        listDirectory(path) match
          case Left(err)      => ToolResult.error(err)
          case Right(entries) =>
            val lines = entries.map: e =>
              val kind = if e.isDirectory then "[DIR] " else "[FILE]"
              val size = e.sizeBytes.map(b => s"  (${formatSize(b)})").getOrElse("")
              s"$kind  ${e.name}$size"
            ToolResult.text(s"Contents of $path (${entries.size} entries):\n" + lines.mkString("\n"))

val readFileTool = tool("read_file")
  .description("Reads the full content of a text file on the local filesystem. Returns an error for binary files or files larger than 1 MB.")
  .input[ReadFileInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        readFile(path) match
          case Left(err) => ToolResult.error(err)
          case Right(fc) => ToolResult.text(s"File: ${fc.path}\nSize: ${formatSize(fc.sizeBytes)}\n\n${fc.text}")

val searchInFilesTool = tool("search_in_files")
  .description(
    "Searches for a regex pattern inside files of a directory (recursively). " +
      "Use fileExtension to filter by extension (e.g. 'scala', 'md', '*' for all). " +
      "Returns matching lines with file path and line number."
  )
  .input[SearchInFilesInput]
  .handle: input =>
    safePath(input.directory) match
      case Left(err)       => ToolResult.error(err)
      case Right(basePath) =>
        searchInFiles(basePath, input.pattern, input.fileExtension, input.maxResults) match
          case Left(err)      => ToolResult.error(err)
          case Right(Nil)     => ToolResult.text(s"No matches found for pattern '${input.pattern}' in $basePath")
          case Right(matches) =>
            val lines  = matches.map(m => s"${m.file}:${m.lineNumber}:  ${m.line}")
            val header = s"Found ${matches.size} match(es) for '${input.pattern}' in $basePath:\n\n"
            ToolResult.text(header + lines.mkString("\n"))

val fileInfoTool = tool("file_info")
  .description("Returns metadata about a file or directory: size, last modified date, permissions.")
  .input[FileInfoInput]
  .handle: input =>
    safePath(input.path) match
      case Left(err)   => ToolResult.error(err)
      case Right(path) =>
        fileInfo(path) match
          case Left(err) => ToolResult.error(err)
          case Right(m)  =>
            val size = m.sizeBytes.map(formatSize).getOrElse("-")
            ToolResult.text(s"""Path:          ${m.path}
Kind:          ${if m.isDirectory then "directory" else "file"}
Size:          $size
Last modified: ${m.lastModified}
Readable:      ${m.readable}
Writable:      ${m.writable}""")

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
