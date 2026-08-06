import chimp.client.*
import chimp.client.transport.ClientHttpTransport
import chimp.protocol.*
import chimp.server.{StreamingMcpServer, ToolResult, tool}
import io.circe.Json
import munit.FunSuite
import ox.*
import sttp.client4.DefaultSyncBackend
import sttp.model.Uri.UriContext
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer

import java.net.{HttpURLConnection, ServerSocket, URL}
import java.util.concurrent.CountDownLatch
import scala.annotation.tailrec
import scala.util.Try

class FilesystemMcpServerTest extends FunSuite:

  val port: Int =
    val s = ServerSocket(0)
    try s.getLocalPort finally s.close()

  var stopServer: () => Unit = () => ()

  override def beforeAll(): Unit =
    val ready    = CountDownLatch(1)
    val shutdown = CountDownLatch(1)

    Thread.ofVirtual().start: () =>
      supervised:
        val server = StreamingMcpServer[Identity]()
          .addTool(listDirTool)
          .addTool(readFileTool)
          .addTool(searchInFilesTool)
          .addTool(fileInfoTool)

        val endpoint = chimp.server.ox.OxServerHttpTransport(List("mcp")).serve(server)
        val binding  = NettySyncServer().port(port).addEndpoint(endpoint).start()
        stopServer = () =>
          binding.stop()
          shutdown.countDown()
        ready.countDown()
        shutdown.await()

    ready.await()
    waitForServer(s"http://localhost:$port/mcp")

  override def afterAll(): Unit =
    stopServer()

  @tailrec
  private def waitForServer(url: String, attempts: Int = 30): Unit =
    val reachable = Try {
      val conn = URL(url).openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("GET")
      conn.setConnectTimeout(300)
      conn.setReadTimeout(300)
      conn.getResponseCode < 500
    }.getOrElse(false)
    if !reachable then
      if attempts <= 0 then throw RuntimeException(s"Server at $url did not start in time")
      Thread.sleep(200)
      waitForServer(url, attempts - 1)

  def withClient[A](f: McpClient[Identity] => A): A =
    val backend   = DefaultSyncBackend()
    val transport = ClientHttpTransport[Identity](backend, uri"http://localhost:$port/mcp")
    val client    = McpClient[Identity](transport, Implementation("test-client", "0.0.1"))
    try f(client)
    finally
      client.close()
      backend.close()

  def textOf(result: CallToolResult): String =
    result.content.collect { case ToolContent.Text(_, text) => text }.mkString

  // --- Testdaten ---

  val tmpDir: os.Path = os.temp.dir(prefix = "mcp-test-")
  val tmpFile         = tmpDir / "hello.txt"
  os.write(tmpFile, "Hello, MCP!\nLine two\nLine three")
  os.makeDir(tmpDir / "sub")
  os.write(tmpDir / "sub" / "nested.txt", "nested content with keyword FIND_ME here")

  // --- Tests ---

  test("list_directory gibt Verzeichnisinhalt zurück"):
    withClient: client =>
      val text = textOf(client.callTool("list_directory", Json.obj("path" -> Json.fromString(tmpDir.toString))))
      assert(text.contains("hello.txt"), s"Erwartet 'hello.txt' in:\n$text")
      assert(text.contains("[DIR]"),     s"Erwartet '[DIR]' in:\n$text")
      assert(text.contains("sub"),       s"Erwartet 'sub' in:\n$text")

  test("list_directory meldet Fehler bei nicht-existentem Pfad"):
    withClient: client =>
      assert(client.callTool("list_directory", Json.obj("path" -> Json.fromString("/pfad/existiert/nicht"))).isError)

  test("read_file liest Dateiinhalt korrekt"):
    withClient: client =>
      val text = textOf(client.callTool("read_file", Json.obj("path" -> Json.fromString(tmpFile.toString))))
      assert(text.contains("Hello, MCP!"), s"Erwartet Dateiinhalt in:\n$text")
      assert(text.contains("Line two"),    s"Erwartet 'Line two' in:\n$text")

  test("read_file meldet Fehler bei Verzeichnis-Pfad"):
    withClient: client =>
      assert(client.callTool("read_file", Json.obj("path" -> Json.fromString(tmpDir.toString))).isError)

  test("search_in_files findet Treffer per Regex"):
    withClient: client =>
      val text = textOf:
        client.callTool(
          "search_in_files",
          Json.obj(
            "directory"     -> Json.fromString(tmpDir.toString),
            "pattern"       -> Json.fromString("FIND_ME"),
            "fileExtension" -> Json.fromString("txt"),
            "maxResults"    -> Json.fromInt(50)
          )
        )
      assert(text.contains("nested.txt"), s"Erwartet 'nested.txt' in:\n$text")
      assert(text.contains("FIND_ME"),    s"Erwartet 'FIND_ME' in:\n$text")

  test("search_in_files meldet keine Treffer wenn Muster fehlt"):
    withClient: client =>
      val text = textOf:
        client.callTool(
          "search_in_files",
          Json.obj(
            "directory"     -> Json.fromString(tmpDir.toString),
            "pattern"       -> Json.fromString("GIBT_ES_NICHT_XYZ"),
            "fileExtension" -> Json.fromString("*"),
            "maxResults"    -> Json.fromInt(50)
          )
        )
      assert(text.contains("No matches found"), s"Erwartet 'No matches found' in:\n$text")

  test("file_info gibt Metadaten für Datei zurück"):
    withClient: client =>
      val text = textOf(client.callTool("file_info", Json.obj("path" -> Json.fromString(tmpFile.toString))))
      assert(text.contains("file"),      s"Erwartet 'file' in:\n$text")
      assert(text.contains("hello.txt"), s"Erwartet Dateiname in:\n$text")
      assert(text.contains("Readable"),  s"Erwartet 'Readable' in:\n$text")

  test("file_info gibt Metadaten für Verzeichnis zurück"):
    withClient: client =>
      val text = textOf(client.callTool("file_info", Json.obj("path" -> Json.fromString(tmpDir.toString))))
      assert(text.contains("directory"), s"Erwartet 'directory' in:\n$text")
