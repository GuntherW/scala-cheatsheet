import chimp.protocol.*
import munit.FunSuite

class McpResourcesAndPromptsTest extends FunSuite:

  val server = McpServerFixture:
    _.addResource(readmeResource)
      .addResourceTemplate(fileResourceTemplate)
      .addPrompt(explainFilePrompt)

  override def munitFixtures = List(server)

  // --- Resources ---

  test("resources/list enthält die README-Resource"):
    val result = server().listResources()
    assert(result.resources.exists(_.uri == "file:///readme"), s"Erwartet 'file:///readme' in: ${result.resources}")

  test("resources/templates/list enthält das Datei-Template"):
    val result = server().listResourceTemplates()
    assert(
      result.resourceTemplates.exists(_.uriTemplate == "file:///{filename}"),
      s"Erwartet 'file:///{filename}' in: ${result.resourceTemplates}"
    )

  test("resources/read liefert den Inhalt der README"):
    val result = server().readResource("file:///readme")
    val text   = result.contents.collect { case ResourceContents.Text(_, text, _, _) => text }.mkString
    assert(text.contains("Filesystem MCP Server"), s"Erwartet README-Inhalt in:\n$text")

  test("resources/read über Template liefert Inhalt einer Datei im Projekt-Root"):
    val result = server().readResource("file:///.scalafmt.conf")
    val text   = result.contents.collect { case ResourceContents.Text(_, text, _, _) => text }.mkString
    assert(text.contains("maxColumn"), s"Erwartet Inhalt von .scalafmt.conf in:\n$text")

  test("resources/read meldet Fehler bei nicht-existenter Datei"):
    intercept[Exception](server().readResource("file:///gibt_es_nicht.xyz"))

  // --- Prompts ---

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
