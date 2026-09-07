import chimp.client.McpProtocolException
import chimp.protocol.*
import munit.FunSuite

class FilesystemMcpServerResourcesTest extends FunSuite:

  val client = McpServerFixture:
    _.addResource(readmeResource)
      .addResourceTemplate(fileResourceTemplate)

  override def munitFixtures = List(client)

  test("resources/list enthält die README-Resource"):
    val result = client().listResources()
    assert(result.resources.exists(_.uri == "file:///readme"), s"Erwartet 'file:///readme' in: ${result.resources}")

  test("resources/templates/list enthält das Datei-Template"):
    val result = client().listResourceTemplates()
    assert(
      result.resourceTemplates.exists(_.uriTemplate == "file:///{filename}"),
      s"Erwartet 'file:///{filename}' in: ${result.resourceTemplates}"
    )

  test("resources/read liefert den Inhalt der README"):
    val result = client().readResource("file:///readme")
    val text   = result.contents.collect { case ResourceContents.Text(_, text, _, _) => text }.mkString
    assert(text.contains("Filesystem MCP Server"), s"Erwartet README-Inhalt in:\n$text")

  test("resources/read über Template liefert Inhalt einer Datei im Projekt-Root"):
    val result = client().readResource("file:///.scalafmt.conf")
    val text   = result.contents.collect { case ResourceContents.Text(_, text, _, _) => text }.mkString
    assert(text.contains("maxColumn"), s"Erwartet Inhalt von .scalafmt.conf in:\n$text")

  test("resources/read meldet Fehler bei nicht-existenter Datei"):
    intercept[McpProtocolException](client().readResource("file:///gibt_es_nicht.xyz"))
