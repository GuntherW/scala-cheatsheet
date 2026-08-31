import chimp.client.*
import chimp.client.transport.ClientHttpTransport
import chimp.protocol.*
import chimp.server.StreamingMcpServer
import chimp.server.ox.OxServerHttpTransport
import io.circe.Decoder
import io.circe.syntax.*
import munit.FunSuite
import os.Path
import ox.{supervised, useInScope}
import sttp.client4.{DefaultSyncBackend, SyncBackend}
import sttp.model.Uri.UriContext
import sttp.shared.Identity
import sttp.tapir.server.netty.sync.NettySyncServer

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import scala.util.chaining.scalaUtilChainingOps
import scala.util.Try

class FilesystemMcpServerTest extends FunSuite:

  // --- Testdaten ---
  val tmpDir: Path  = os.temp.dir(prefix = "mcp-test-")
  val tmpFile: Path = tmpDir / "hello.txt"
  os.write(tmpFile, "Hello, MCP!\nLine two\nLine three")
  os.makeDir(tmpDir / "sub")
  os.write(tmpDir / "sub" / "nested.txt", "nested content with keyword FIND_ME here")

  // --- Server + Client Lifecycle ---
  var client: McpClient[Identity] = null
  var serverShutdown: () => Unit  = () => ()
  var backend: SyncBackend        = null

  override def beforeAll(): Unit =
    val ready    = CountDownLatch(1)
    val shutdown = CountDownLatch(1)
    val portRef  = new AtomicInteger(0)

    Thread.ofVirtual().start: () =>
      supervised:
        val endpoint = OxServerHttpTransport(List("mcp")).serve:
          StreamingMcpServer[Identity]()
            .addTool(listDirTool)
            .addTool(readFileTool)
            .addTool(searchInFilesTool)
            .addTool(fileInfoTool)
        val binding  = useInScope(NettySyncServer().port(0).addEndpoint(endpoint).start())(_.stop())
        portRef.set(binding.port)
        serverShutdown = () => shutdown.countDown()
        ready.countDown()
        shutdown.await()

    ready.await()
    val port = portRef.get()

    backend = DefaultSyncBackend()
    val transport = ClientHttpTransport[Identity](backend, uri"http://localhost:$port/mcp")
    client = Iterator.continually(Try(McpClient[Identity](transport, Implementation("test-client", "0.0.1"))))
      .flatMap(_.toOption)
      .tap(_ => Thread.sleep(50))
      .next()

  override def afterAll(): Unit =
    client.close()
    backend.close()
    serverShutdown()
//    os.remove.all(tmpDir)

  // --- Hilfsfunktionen ---
  extension (result: CallToolResult)
    def asText: String    = result.content.collect { case ToolContent.Text(_, text) => text }.mkString
    def as[A: Decoder]: A =
      result.structuredContent
        .toRight("no structuredContent in result")
        .flatMap(_.as[A])
        .fold(e => throw AssertionError(e.toString), identity)

  // --- Tests ---
  test("list_directory gibt strukturierten Verzeichnisinhalt zurück"):
    val result = client.callTool("list_directory", ListDirInput(tmpDir.toString).asJson)
    assert(!result.isError)
    val output = result.as[DirListOutput]
    assert(output.entries.exists(_.name == "hello.txt"), s"Erwartet 'hello.txt' in: $output")
    assert(output.entries.exists(_.kind == "directory"), s"Erwartet kind=directory in: $output")
    assert(output.entries.exists(_.name == "sub"), s"Erwartet 'sub' in: $output")

  test("list_directory meldet Fehler bei nicht-existentem Pfad"):
    assert(client.callTool("list_directory", ListDirInput("/pfad/existiert/nicht").asJson).isError)

  test("read_file liest Dateiinhalt korrekt"):
    val text = client.callTool("read_file", ReadFileInput(tmpFile.toString).asJson).asText
    assert(text.contains("Hello, MCP!"), s"Erwartet Dateiinhalt in:\n$text")
    assert(text.contains("Line two"), s"Erwartet 'Line two' in:\n$text")

  test("read_file meldet Fehler bei Verzeichnis-Pfad"):
    assert(client.callTool("read_file", ReadFileInput(tmpDir.toString).asJson).isError)

  test("search_in_files gibt strukturierte Treffer zurück"):
    val result = client.callTool(
      "search_in_files",
      SearchInFilesInput(tmpDir.toString, "FIND_ME", fileExtension = "txt", maxResults = 50).asJson
    )
    assert(!result.isError)
    val output = result.as[SearchResultOutput]
    assert(output.matches.exists(_.file.contains("nested.txt")), s"Erwartet 'nested.txt' in: $output")
    assert(output.matches.exists(_.text.contains("FIND_ME")), s"Erwartet 'FIND_ME' in: $output")

  test("search_in_files meldet keine Treffer wenn Muster fehlt"):
    val result = client.callTool("search_in_files", SearchInFilesInput(tmpDir.toString, "GIBT_ES_NICHT_XYZ").asJson)
    assert(!result.isError)
    assertEquals(result.as[SearchResultOutput].count, 0)

  test("file_info gibt strukturierte Metadaten für Datei zurück"):
    val result = client.callTool("file_info", FileInfoInput(tmpFile.toString).asJson)
    assert(!result.isError)
    val output = result.as[FileInfoOutput]
    assertEquals(output.kind, "file")
    assertEquals(output.readable, true)
    assert(output.path.contains("hello.txt"), s"Erwartet 'hello.txt' in: ${output.path}")

  test("file_info gibt strukturierte Metadaten für Verzeichnis zurück"):
    val result = client.callTool("file_info", FileInfoInput(tmpDir.toString).asJson)
    assert(!result.isError)
    assertEquals(result.as[FileInfoOutput].kind, "directory")
