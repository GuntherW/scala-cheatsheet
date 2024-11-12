package de.codecentric.wittig.scala.testContainer

import java.net.URI

import scala.io.Source
import scala.language.adhocExtensions

import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.tagobjects.Slow
import org.testcontainers.containers.wait.strategy.Wait

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
    val nginxUri      = new URI(s"http://${container.containerIpAddress}:${container.mappedPort(80)}/")
    val nginxResponse = Source
      .fromInputStream(nginxUri.toURL.openConnection().getInputStream)
      .mkString
    assert(nginxResponse.contains("If you see this page, the nginx web server is successfully installed"))
  }
