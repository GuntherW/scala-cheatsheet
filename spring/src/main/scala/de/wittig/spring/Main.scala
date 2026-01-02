package de.wittig.spring

import tools.jackson.databind.{json, ObjectMapper}
import tools.jackson.module.scala.DefaultScalaModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

object Main:
  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[Main], args*)

@SpringBootApplication
class Main:

  @Bean
  def restClient(builder: RestClient.Builder): RestClient =
    builder.build

  @Bean
  def objectMapper(): ObjectMapper =
    json.JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .build()
