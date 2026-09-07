import chimp.protocol.*
import munit.FunSuite

class FilesystemMcpServerResourcesTest extends FunSuite:

  val server = McpServerFixture:
    _.addResource(readmeResource)
      .addResourceTemplate(fileResourceTemplate)

  override def munitFixtures = List(server)

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
