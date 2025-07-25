import JsonMappingTest.{Model1, Model2}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import munit.FunSuite

class JsonMappingTest extends FunSuite:

  test("model1 1"):
    val mapper = ObjectMapper().registerModules(DefaultScalaModule, new JavaTimeModule)

    val model = Model1(1, "name")
    val json  = mapper.writeValueAsString(model)
    assertEquals(json, """{"id":1,"name":"name"}""")

  test("model1 2"):
    val mapper = ObjectMapper().registerModules(DefaultScalaModule, new JavaTimeModule)

    val json  = """{"id":1}"""
    val model = Model1(1, null)
    val m     = mapper.readValue(json, classOf[Model1])
    assertEquals(m, model)

  test("model2 1"):
    val mapper = ObjectMapper().registerModules(DefaultScalaModule, new JavaTimeModule)

    val model = Model2(1, List("name"))
    val json  = mapper.writeValueAsString(model)
    assertEquals(json, """{"id":1,"li":["name"]}""")

  test("model2 2"):
    val mapper = ObjectMapper().registerModules(DefaultScalaModule, new JavaTimeModule)
//      .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
//      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)

    val json  = """{"id":1}"""
    val model = Model2(1, Nil)
    val m     = mapper.readValue(json, classOf[Model2])
    assertEquals(m, model)

object JsonMappingTest:
  case class Model1(id: Long, name: String)
  case class Model2(id: Long, li: List[String])
