package de.wittig.config.pure

import com.typesafe.config.ConfigFactory
import pureconfig.*

import scala.util.chaining.scalaUtilChainingOps

object MainRuntime extends App {

  case class MyConf(runtimetesta: String, runtimetestb: String) derives ConfigReader

  System.setProperty("RUNTIME_TESTB", "Auch Hallo")
  println(sys.props.get("RUNTIME_TESTB"))

  lazy val resource = ConfigSource.fromConfig(ConfigFactory.load("application.conf"))
  resource.load[MyConf].tap(println)
}
