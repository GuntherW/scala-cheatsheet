package de.codecentric.wittig.scala.zio.tut.config

final case class Configuration(eins: EinsConfig)
final case class EinsConfig(inner: InnerEinsConfig)
final case class InnerEinsConfig(wert: String)
