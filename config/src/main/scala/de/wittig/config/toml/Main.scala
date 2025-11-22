package de.wittig.config.toml
import toml.derivation.auto.*

import scala.util.chaining.scalaUtilChainingOps

@main
def main(): Unit =

  val table =
    """
      |a = 1
      |[table]
      |b = 2
    """.stripMargin

  toml.Toml.parse(table).tap(println)
  toml.Toml.parseAs[Root1](table).tap(println)

  val tableList =
    """
      |points = [ { x = 1, y = 2, z = 3 },
      |           { x = 7, y = 8, z = 9 },
      |           { x = 2, y = 4, z = 8 } ]
    """.stripMargin

  toml.Toml.parseAs[Root2](tableList).tap(println)

  // New Lines and Trainling commas supported via Extension:
  toml.Toml.parse(
    """key = {
    a = 23,
    b = 42,
  }""",
    Set(toml.Extension.MultiLineInlineTables)
  ).tap(println)

case class Root1(a: Int, table: Table)
case class Table(b: Int)

case class Root2(points: List[Point])
case class Point(x: Int, y: Int, z: Int)
