import chimp.protocol.*
import io.circe.Decoder
import io.circe.syntax.*
import munit.FunSuite
import os.Path

class FilesystemMcpServerToolsTest extends FunSuite:

  val server = McpServerFixture:
    _.addTool(listDirTool)
      .addTool(readFileTool)
      .addTool(writeFileTool)
      .addTool(editFileTool)
      .addTool(searchInFilesTool)
      .addTool(globTool)
      .addTool(fileInfoTool)
      .addTool(createDirectoryTool)
      .addTool(moveTool)
      .addTool(copyTool)

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

  // --- write_file ---
  test("write_file erstellt neue Datei"):
    val newFile = tmpDir / "created.txt"
    val result  = server().callTool("write_file", WriteFileInput(newFile.toString, "hello write").asJson)
    assert(!result.isError)
    assert(os.exists(newFile))
    assertEquals(os.read(newFile), "hello write")

  test("write_file überschreibt bestehende Datei"):
    val f = tmpDir / "overwrite.txt"
    os.write(f, "old content")
    assert(!server().callTool("write_file", WriteFileInput(f.toString, "new content").asJson).isError)
    assertEquals(os.read(f), "new content")

  test("write_file erstellt fehlende Verzeichnisse"):
    val deep = tmpDir / "a" / "b" / "c.txt"
    assert(!server().callTool("write_file", WriteFileInput(deep.toString, "deep").asJson).isError)
    assert(os.exists(deep))

  // --- edit_file ---
  test("edit_file ersetzt eindeutigen String"):
    val f = tmpDir / "edit.txt"
    os.write(f, "foo bar baz")
    assert(!server().callTool("edit_file", EditFileInput(f.toString, "bar", "QUX").asJson).isError)
    assertEquals(os.read(f), "foo QUX baz")

  test("edit_file meldet Fehler wenn oldString nicht gefunden"):
    val f = tmpDir / "edit2.txt"
    os.write(f, "some content")
    assert(server().callTool("edit_file", EditFileInput(f.toString, "NICHT_DA", "x").asJson).isError)

  test("edit_file meldet Fehler bei mehrfachem Match"):
    val f = tmpDir / "edit3.txt"
    os.write(f, "x x x")
    assert(server().callTool("edit_file", EditFileInput(f.toString, "x", "y").asJson).isError)

  // --- glob ---
  test("glob findet Dateien nach Erweiterung"):
    val result = server().callTool("glob", GlobInput(tmpDir.toString, "*.txt").asJson)
    assert(!result.isError)
    val output = result.as[GlobOutput]
    assert(output.files.exists(_.contains("hello.txt")), s"Erwartet 'hello.txt' in: $output")

  test("glob gibt leere Liste bei keinem Treffer"):
    val result = server().callTool("glob", GlobInput(tmpDir.toString, "*.xyz").asJson)
    assert(!result.isError)
    assertEquals(result.as[GlobOutput].count, 0)

  // --- create_directory ---
  test("create_directory legt Verzeichnis an"):
    val newDir = tmpDir / "newdir" / "sub"
    assert(!server().callTool("create_directory", CreateDirectoryInput(newDir.toString).asJson).isError)
    assert(os.isDir(newDir))

  // --- move ---
  test("move verschiebt eine Datei"):
    val src = tmpDir / "move_src.txt"
    val dst = tmpDir / "move_dst.txt"
    os.write(src, "move me")
    assert(!server().callTool("move", MoveInput(src.toString, dst.toString).asJson).isError)
    assert(!os.exists(src))
    assert(os.exists(dst))
    assertEquals(os.read(dst), "move me")

  test("move meldet Fehler bei nicht-existenter Quelle"):
    assert(server().callTool("move", MoveInput((tmpDir / "ghost.txt").toString, (tmpDir / "x.txt").toString).asJson).isError)

  // --- copy ---
  test("copy kopiert eine Datei"):
    val src = tmpDir / "copy_src.txt"
    val dst = tmpDir / "copy_dst.txt"
    os.write(src, "copy me")
    assert(!server().callTool("copy", CopyInput(src.toString, dst.toString).asJson).isError)
    assert(os.exists(src))
    assert(os.exists(dst))
    assertEquals(os.read(dst), "copy me")

  // --- read_file mit offset/limit ---
  test("read_file liest mit offset und limit"):
    val text = server().callTool("read_file", ReadFileInput(tmpFile.toString, offset = 2, limit = Some(1)).asJson).asText
    assert(text.contains("Line two"), s"Erwartet 'Line two' in:\n$text")
    assert(!text.contains("Hello, MCP!"), s"Erwartet keine erste Zeile in:\n$text")

  // --- Hilfsfunktionen ---
  extension (result: CallToolResult)
    def asText: String    = result.content.collect { case ToolContent.Text(_, text) => text }.mkString
    def as[A: Decoder]: A =
      result.structuredContent
        .toRight("no structuredContent in result")
        .flatMap(_.as[A])
        .fold(e => throw AssertionError(e.toString), identity)
