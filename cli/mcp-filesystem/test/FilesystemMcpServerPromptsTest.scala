import chimp.protocol.*
import munit.FunSuite

class FilesystemMcpServerPromptsTest extends FunSuite:

  val server = McpServerFixture:
    _.addPrompt(explainFilePrompt)

  override def munitFixtures = List(server)

  test("prompts/list enthält explain_file mit Pflicht-Argument path"):
    val result = server().listPrompts()
    val prompt = result.prompts.find(_.name == "explain_file")
    assert(prompt.isDefined, s"Erwartet 'explain_file' in: ${result.prompts}")
    assert(prompt.get.arguments.exists(_.exists(a => a.name == "path" && a.required.contains(true))))

  test("prompts/get liefert eine User-Nachricht mit dem übergebenen Pfad"):
    val result = server().getPrompt("explain_file", Map("path" -> "/tmp/foo.scala"))
    assertEquals(result.messages.size, 1)
    val message = result.messages.head
    assertEquals(message.role, Role.User)
    val text = message.content match
      case ToolContent.Text(_, text) => text
      case other                     => fail(s"Erwartet Text-Content, war: $other")
    assert(text.contains("/tmp/foo.scala"), s"Erwartet Pfad im Prompt-Text: $text")
