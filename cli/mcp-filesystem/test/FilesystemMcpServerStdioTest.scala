import chimp.client.*
import chimp.client.transport.ClientStdioTransport
import chimp.protocol.*
import io.circe.syntax.*
import munit.FunSuite
import sttp.shared.Identity

/** Testet den Server über den echten stdio-Transport (Subprozess), also genau den Pfad, den
  * OpenCode nutzt (siehe `@main def filesystemMcpServerStdio` in `FilesystemMcpServer.scala`
  * und README.md, Abschnitt "stdio-Modus"). Anders als die übrigen Tests läuft hier kein
  * in-process HTTP-Server, sondern ein komplett separater JVM-Prozess, mit dem über
  * stdin/stdout JSON-RPC-Zeilen ausgetauscht werden.
  *
  * Der Prozess wird mit der aktuellen Test-Classpath gestartet (kein erneutes Kompilieren nötig),
  * da `filesystemMcpServerStdio` bereits Teil der kompilierten Klassen ist.
  */
class FilesystemMcpServerStdioTest extends FunSuite:

  private def javaBin: String   = s"${System.getProperty("java.home")}/bin/java"
  private def classpath: String = System.getProperty("java.class.path")

  private var transport: ClientStdioTransport      = null
  private var client: BidirectionalMcpClient[Identity] = null

  override def beforeAll(): Unit =
    transport = ClientStdioTransport(List(javaBin, "-cp", classpath, "filesystemMcpServerStdio"))
    client = McpClient.bidirectional(transport, Implementation("stdio-test-client", "0.0.1"))

  override def afterAll(): Unit =
    client.close()

  test("Server antwortet über stdio auf tools/list"):
    val tools = client.listTools()
    assert(tools.tools.exists(_.name == "list_directory"), s"Erwartet 'list_directory' in: ${tools.tools.map(_.name)}")

  test("Server antwortet über stdio auf resources/list"):
    val resources = client.listResources()
    assert(resources.resources.exists(_.uri == "file:///readme"))

  test("Server antwortet über stdio auf prompts/list"):
    val prompts = client.listPrompts()
    assert(prompts.prompts.exists(_.name == "explain_file"))

  test("Ein Tool-Aufruf funktioniert Ende-zu-Ende über stdio"):
    val result = client.callTool("file_info", FileInfoInput(os.pwd.toString).asJson)
    assert(!result.isError)

  test("subscribeResource schlägt fehl, da der Server subscribe nicht unterstützt"):
    // Der Server meldet in initialize() resources.subscribe = false (siehe FilesystemMcpServer.scala) —
    // der Client verweigert den Aufruf schon lokal, ohne eine Anfrage zu senden.
    intercept[McpProtocolException](client.subscribeResource("file:///readme"))
