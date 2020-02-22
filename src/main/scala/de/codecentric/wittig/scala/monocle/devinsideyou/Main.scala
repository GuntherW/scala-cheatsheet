package de.codecentric.wittig.scala.monocle.devinsideyou

import monocle.Iso
import monocle.macros.GenLens
import monocle.macros.syntax.lens._

object Main extends App {

  MyIso.run
  MyLens.run
  MyPrism.run
}
