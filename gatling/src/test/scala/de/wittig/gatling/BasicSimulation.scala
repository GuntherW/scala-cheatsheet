package de.wittig.gatling

import scala.concurrent.duration.*

import io.gatling.core.Predef.*
import io.gatling.http.Predef.*

class BasicSimulation extends Simulation:

  private val httpProtocol = http
    .baseUrl("http://computer-database.gatling.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  private val request = http("request_1").get("/")

  private val scn = scenario("BasicSimulation")
    .exec(request)
    .pause(5)

  setUp(
    scn.inject(atOnceUsers(10))
  ).protocols(httpProtocol)
    .throttle(
      reachRps(10).in(10),
      holdFor(10)
    )
