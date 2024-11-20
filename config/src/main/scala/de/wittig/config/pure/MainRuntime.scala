package de.wittig.config.pure

import scala.util.chaining.scalaUtilChainingOps

import com.typesafe.config.ConfigFactory
import pureconfig.*
import pureconfig.generic.derivation.default.*
import pureconfig.generic.semiauto.*

object MainRuntime extends App {

  case class MyConf(runtimetesta: String, runtimetestb: String) derives ConfigReader

  System.setProperty("RUNTIME_TESTB", "Auch Hallo")
  println(sys.props.get("RUNTIME_TESTB"))

  lazy val resource = ConfigSource.fromConfig(ConfigFactory.load("application.conf"))
  resource.load[MyConf].tap(println)
}
