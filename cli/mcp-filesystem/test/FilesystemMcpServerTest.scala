import chimp.client.*
import chimp.client.transport.ClientHttpTransport
import chimp.protocol.*
import chimp.server.StreamingMcpServer
import io.circe.syntax.*
import munit.{FunFixtures, FunSuite}
import ox.*
import sttp.client4.DefaultSyncBackend
import sttp.model.Uri.UriContext
import sttp.shared.Identity
import sttp.tapir.server.netty.sync.NettySyncServer

import java.net.ServerSocket

class FilesystemMcpServerTest extends FunSuite, FunFixtures:

  // --- Testdaten ---
  val tmpDir: os.Path = os.temp.dir(prefix = "mcp-test-")
  val tmpFile         = tmpDir / "hello.txt"
  os.write(tmpFile, "Hello, MCP!\nLine two\nLine three")
  os.makeDir(tmpDir / "sub")
  os.write(tmpDir / "sub" / "nested.txt", "nested content with keyword FIND_ME here")

  // --- Server einmal für alle Tests starten ---
  val port: Int =
    val s = ServerSocket(0)
    try s.getLocalPort
    finally s.close()

  var serverFork: CancellableFork[Unit] = null

  override def beforeAll(): Unit =
    val endpoint = chimp.server.ox.OxServerHttpTransport(List("mcp")).serve:
      StreamingMcpServer[Identity]()
        .addTool(listDirTool)
        .addTool(readFileTool)
        .addTool(searchInFilesTool)
        .addTool(fileInfoTool)

    val ready = java.util.concurrent.CountDownLatch(1)

    Thread.ofVirtual().start: () =>
      unsupervised:
        serverFork = forkCancellable:
          supervised:
            useInScope(NettySyncServer().port(port).addEndpoint(endpoint).start())(_.stop()).discard
            never
        ready.countDown()
        serverFork.join()

    ready.await()

  override def afterAll(): Unit =
    serverFork.cancel()

  // --- Client-Fixture: frischer Client pro Test, Server läuft durch ---
  val mcpClient = FunFixture[McpClient[Identity]](
    setup = _ =>
      val backend   = DefaultSyncBackend()
      val transport = ClientHttpTransport[Identity](backend, uri"http://localhost:$port/mcp")
      Iterator.continually(scala.util.Try(McpClient[Identity](transport, Implementation("test-client", "0.0.1"))))
        .flatMap(_.toOption)
        .tap(_ => Thread.sleep(50))
        .next()
    ,
    teardown = _.close()
  )

  // --- Hilfsfunktionen ---

  extension (result: CallToolResult)
    def asText: String             = result.content.collect { case ToolContent.Text(_, text) => text }.mkString
    def as[A: io.circe.Decoder]: A =
      result.structuredContent
        .toRight("no structuredContent in result")
        .flatMap(_.as[A])
        .fold(e => throw AssertionError(e.toString), identity)

  // --- Tests ---

  mcpClient.test("list_directory gibt strukturierten Verzeichnisinhalt zurück"): client =>
    val result = client.callTool("list_directory", ListDirInput(tmpDir.toString).asJson)
    assert(!result.isError)
    val output = result.as[DirListOutput]
    assert(output.entries.exists(_.name == "hello.txt"), s"Erwartet 'hello.txt' in: $output")
    assert(output.entries.exists(_.kind == "directory"), s"Erwartet Eintrag mit kind=directory in: $output")
    assert(output.entries.exists(_.name == "sub"), s"Erwartet 'sub' in: $output")

  mcpClient.test("list_directory meldet Fehler bei nicht-existentem Pfad"): client =>
    assert(client.callTool("list_directory", ListDirInput("/pfad/existiert/nicht").asJson).isError)

  mcpClient.test("read_file liest Dateiinhalt korrekt"): client =>
    val text = client.callTool("read_file", ReadFileInput(tmpFile.toString).asJson).asText
    assert(text.contains("Hello, MCP!"), s"Erwartet Dateiinhalt in:\n$text")
    assert(text.contains("Line two"), s"Erwartet 'Line two' in:\n$text")

  mcpClient.test("read_file meldet Fehler bei Verzeichnis-Pfad"): client =>
    assert(client.callTool("read_file", ReadFileInput(tmpDir.toString).asJson).isError)

  mcpClient.test("search_in_files gibt strukturierte Treffer zurück"): client =>
    val result = client.callTool(
      "search_in_files",
      SearchInFilesInput(tmpDir.toString, "FIND_ME", fileExtension = "txt", maxResults = 50).asJson
    )
    assert(!result.isError)
    val output = result.as[SearchResultOutput]
    assert(output.matches.exists(_.file.contains("nested.txt")), s"Erwartet 'nested.txt' in: $output")
    assert(output.matches.exists(_.text.contains("FIND_ME")), s"Erwartet 'FIND_ME' in: $output")

  mcpClient.test("search_in_files meldet keine Treffer wenn Muster fehlt"): client =>
    val result = client.callTool(
      "search_in_files",
      SearchInFilesInput(tmpDir.toString, "GIBT_ES_NICHT_XYZ").asJson
    )
    assert(!result.isError)
    assertEquals(result.as[SearchResultOutput].count, 0)

  mcpClient.test("file_info gibt strukturierte Metadaten für Datei zurück"): client =>
    val result = client.callTool("file_info", FileInfoInput(tmpFile.toString).asJson)
    assert(!result.isError)
    val output = result.as[FileInfoOutput]
    assertEquals(output.kind, "file")
    assertEquals(output.readable, true)
    assert(output.path.contains("hello.txt"), s"Erwartet 'hello.txt' in: ${output.path}")

  mcpClient.test("file_info gibt strukturierte Metadaten für Verzeichnis zurück"): client =>
    val result = client.callTool("file_info", FileInfoInput(tmpDir.toString).asJson)
    assert(!result.isError)
    assertEquals(result.as[FileInfoOutput].kind, "directory")
