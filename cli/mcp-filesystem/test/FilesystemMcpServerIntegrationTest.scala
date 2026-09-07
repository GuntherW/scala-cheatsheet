import chimp.protocol.*
import io.circe.syntax.*
import munit.FunSuite

/** Demonstriert eine typische Mehrschritt-Interaktion, wie sie ein echter Agent durchführen würde:
  * zuerst einen Prompt abrufen (der einen Dateipfad enthält), dann mit dem darin enthaltenen Pfad
  * tatsächlich ein Tool aufrufen. Prompts liefern hier nur Text — der Client/Agent muss die
  * Tool-Aufrufe selbst anschließen, das übernimmt MCP nicht automatisch.
  */
class FilesystemMcpServerIntegrationTest extends FunSuite:

  val client = McpServerFixture:
    _.addTool(readFileTool)
      .addPrompt(explainFilePrompt)

  override def munitFixtures = List(client)

  val tmpFile: os.Path = os.temp(contents = "def hello() = println(\"hi\")", prefix = "mcp-integration-")

  test("Prompt liefert Pfad, der anschließend per read_file-Tool gelesen werden kann"):
    val promptResult = client().getPrompt("explain_file", Map("path" -> tmpFile.toString))
    val promptText = promptResult.messages.head.content match
      case ToolContent.Text(_, text) => text
      case other                     => fail(s"Erwartet Text-Content, war: $other")
    assert(promptText.contains(tmpFile.toString), s"Erwartet Pfad im Prompt-Text: $promptText")

    // Der Agent müsste den Pfad selbst aus dem Prompt-Text extrahieren — hier simulieren wir das,
    // indem wir ihn direkt weiterverwenden (wir kennen ihn ja schon aus dem Test-Setup).
    val toolResult = client().callTool("read_file", ReadFileInput(tmpFile.toString).asJson)
    assert(!toolResult.isError)
    val text = toolResult.content.collect { case ToolContent.Text(_, text) => text }.mkString
    assert(text.contains("def hello()"), s"Erwartet Dateiinhalt in:\n$text")
