package de.codecentric.wittig.scala.testContainer

import java.net.URL

import scala.io.Source

import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.containers.wait.strategy.Wait
import scala.language.adhocExtensions

import org.scalatest.tagobjects.Slow

/** @see
  *   https://github.com/testcontainers/testcontainers-scala
  *
  * @see
  *   https://dkovalenko.net/testcontainer-aerospike/
  */
class TestNginxViaTestContainer extends AnyFunSuite, ForAllTestContainer:

  override val container: GenericContainer = GenericContainer(
    "nginx:latest",
    exposedPorts = Seq(80),
    waitStrategy = Wait.forHttp("/"),
  )

  ignore("GenericContainer should start nginx and expose 80 port", Slow) {
    val nginxUrl      = new URL(s"http://${container.containerIpAddress}:${container.mappedPort(80)}/")
    val nginxResponse = Source
      .fromInputStream(nginxUrl.openConnection().getInputStream)
      .mkString
    assert(nginxResponse.contains("If you see this page, the nginx web server is successfully installed"))
  }
