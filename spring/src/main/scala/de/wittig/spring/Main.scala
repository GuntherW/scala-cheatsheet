package de.wittig.spring

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

@SpringBootApplication
class Main:

  @Bean
  def restClient(builder: RestClient.Builder): RestClient =
    builder.build

  @Bean
  def objectMapper(): ObjectMapper =
    ObjectMapper()
      .registerModules(DefaultScalaModule, new JavaTimeModule)

object Main:
  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[Main], args*)
