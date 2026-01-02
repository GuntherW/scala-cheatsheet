package de.codecentric.wittig.scala.jackson

import com.fasterxml.jackson.annotation.JsonAnyGetter
import tools.jackson.databind.*
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule

import scala.jdk.CollectionConverters.MapHasAsJava

@main
def main(): Unit =

  val p = Person("lklkj", Map("k1" -> "w1", "k2" -> "w2"))
  println(objectMapper.writeValueAsString(p))

  val p1 = Person2("aaa", Map("k1" -> "w1", "k2" -> "w2"))
  println(objectMapper.writeValueAsString(p1))

case class Person(name: String, properties: Map[String, String])

class Person2(val name: String, properties: Map[String, String]):
  @JsonAnyGetter
  def props: java.util.Map[String, String] = properties.asJava

object objectMapper:
  private val mapper: ObjectMapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .build()
  
  def writeValueAsString(value: Any): String = mapper.writeValueAsString(value)
