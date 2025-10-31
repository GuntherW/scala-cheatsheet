import JsonMappingTest.{Model2, Model3}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import munit.FunSuite

class JsonMappingTest extends FunSuite:

  private val mapper = ObjectMapper()
    .registerModules(DefaultScalaModule, new JavaTimeModule)

  test("model2 1"):
    val model = Model2(1, List("eins"))
    val json  = mapper.writeValueAsString(model)
    assertEquals(json, """{"id":1,"li":["eins"]}""")

  // Genau das ist das Standardverhalten der alten Jacksonversion (2.18.x). Das geht jetzt nicht mehr.
  test("model2 2".ignore):
    val json  = """{"id":1}"""
    val model = Model2(1, null)
    val m     = mapper.readValue(json, classOf[Model2])
    assertEquals(m, model)

  test("model2 3"):
    val json  = """{"id":1, "li":["eins"]}"""
    val model = Model2(1, List("eins"))
    val m     = mapper.readValue(json, classOf[Model2])
    assertEquals(m, model)

  test("model3 1"):
    val json  = """{"id":1}"""
    val model = Model3(1, null)
    val m     = mapper.readValue(json, classOf[Model3])
    assertEquals(m, model)

  test("model3 2"):
    val json  = """{"id":1, "li":["eins"]}"""
    val model = Model3(1, new java.util.ArrayList(java.util.Arrays.asList("eins")))
    val m     = mapper.readValue(json, classOf[Model3])
    assertEquals(m, model)

  test("model3 3"):
    val json  = """{"id":1, "li":[]}"""
    val model = Model3(1, new java.util.ArrayList())
    val m     = mapper.readValue(json, classOf[Model3])
    assertEquals(m, model)

object JsonMappingTest:
  case class Model2(id: Long, li: List[String])
  case class Model3(id: Long, li: java.util.List[String])
