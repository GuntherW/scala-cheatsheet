package de.wittig.config.toml
import toml.*
import Node.*
import Value.*

import scala.util.chaining.scalaUtilChainingOps

@main
def generate(): Unit =

  val root = Root(List(Pair(
    "scalaDeps",
    Arr(List(
      Arr(List(Str("io.monix"), Str("minitest"), Str("2.2.2"))),
      Arr(List(Str("org.scalacheck"), Str("scalacheck"), Str("1.14.0"))),
      Arr(List(Str("org.scalatest"), Str("scalatest"), Str("3.2.0-SNAP10")))
    ))
  )))

  toml.Toml.generate(root).tap(println)
