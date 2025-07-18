package de.wittig.spring.controller
import de.wittig.spring.domain.Message
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RestController}
import org.springframework.web.client.RestClient

import java.time.LocalDate

@RestController
class HelloController(restClient: RestClient):

  @GetMapping(Array("/hello"))
  def hello(): ResponseEntity[Message] =
    val message = Message("Hallo Scala3 mit Spring Boot!", 2, LocalDate.now)
    ResponseEntity.ok(message)

  @GetMapping(Array("/delay/{seconds}"))
  def delay(@PathVariable seconds: Int): ResponseEntity[Map[String, Any]] = {

    // Aufruf eines externen Service um einen blockierenden Thread zu simulieren.
    val res = restClient.get()
      .uri(s"http://localhost:9000/delay/$seconds")
      .retrieve()
      .body(classOf[Map[String, Any]])

    // Map verschiedener Types, die in Json umgewandelt wird.
    val responseMap = Map[String, Any](
      "seconds"          -> seconds,
      "timestamp"        -> System.currentTimeMillis(),
      "date"             -> LocalDate.now().toString,
      "thread"           -> Thread.currentThread().getName,
      "externalResponse" -> res
    )

    ResponseEntity.ok(responseMap)
  }
