import chimp.protocol.*
import io.circe.Decoder
import io.circe.syntax.*
import munit.FunSuite
import os.Path

class FilesystemMcpServerTest extends FunSuite:

  val server = McpServerFixture:
    _.addTool(listDirTool)
      .addTool(readFileTool)
      .addTool(searchInFilesTool)
      .addTool(fileInfoTool)

  override def munitFixtures = List(server)

  // --- Testdaten ---
  val tmpDir: Path  = os.temp.dir(prefix = "mcp-test-")
  val tmpFile: Path = tmpDir / "hello.txt"
  os.write(tmpFile, "Hello, MCP!\nLine two\nLine three")
  os.makeDir(tmpDir / "sub")
  os.write(tmpDir / "sub" / "nested.txt", "nested content with keyword FIND_ME here")

  // --- Tests ---
  test("list_directory gibt strukturierten Verzeichnisinhalt zurück"):
    val result = server().callTool("list_directory", ListDirInput(tmpDir.toString).asJson)
    assert(!result.isError)
    val output = result.as[DirListOutput]
    assert(output.entries.exists(_.name == "hello.txt"), s"Erwartet 'hello.txt' in: $output")
    assert(output.entries.exists(_.kind == "directory"), s"Erwartet kind=directory in: $output")
    assert(output.entries.exists(_.name == "sub"), s"Erwartet 'sub' in: $output")

  test("list_directory meldet Fehler bei nicht-existentem Pfad"):
    assert(server().callTool("list_directory", ListDirInput("/pfad/existiert/nicht").asJson).isError)

  test("read_file liest Dateiinhalt korrekt"):
    val text = server().callTool("read_file", ReadFileInput(tmpFile.toString).asJson).asText
    assert(text.contains("Hello, MCP!"), s"Erwartet Dateiinhalt in:\n$text")
    assert(text.contains("Line two"), s"Erwartet 'Line two' in:\n$text")

  test("read_file meldet Fehler bei Verzeichnis-Pfad"):
    assert(server().callTool("read_file", ReadFileInput(tmpDir.toString).asJson).isError)

  test("search_in_files gibt strukturierte Treffer zurück"):
    val result = server().callTool(
      "search_in_files",
      SearchInFilesInput(tmpDir.toString, "FIND_ME", fileExtension = "txt", maxResults = 50).asJson
    )
    assert(!result.isError)
    val output = result.as[SearchResultOutput]
    assert(output.matches.exists(_.file.contains("nested.txt")), s"Erwartet 'nested.txt' in: $output")
    assert(output.matches.exists(_.text.contains("FIND_ME")), s"Erwartet 'FIND_ME' in: $output")

  test("search_in_files meldet keine Treffer wenn Muster fehlt"):
    val result = server().callTool("search_in_files", SearchInFilesInput(tmpDir.toString, "GIBT_ES_NICHT_XYZ").asJson)
    assert(!result.isError)
    assertEquals(result.as[SearchResultOutput].count, 0)

  test("file_info gibt strukturierte Metadaten für Datei zurück"):
    val result = server().callTool("file_info", FileInfoInput(tmpFile.toString).asJson)
    assert(!result.isError)
    val output = result.as[FileInfoOutput]
    assertEquals(output.kind, "file")
    assertEquals(output.readable, true)
    assert(output.path.contains("hello.txt"), s"Erwartet 'hello.txt' in: ${output.path}")

  test("file_info gibt strukturierte Metadaten für Verzeichnis zurück"):
    val result = server().callTool("file_info", FileInfoInput(tmpDir.toString).asJson)
    assert(!result.isError)
    assertEquals(result.as[FileInfoOutput].kind, "directory")

  // --- Hilfsfunktionen ---
  extension (result: CallToolResult)
    def asText: String    = result.content.collect { case ToolContent.Text(_, text) => text }.mkString
    def as[A: Decoder]: A =
      result.structuredContent
        .toRight("no structuredContent in result")
        .flatMap(_.as[A])
        .fold(e => throw AssertionError(e.toString), identity)
