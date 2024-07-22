package de.wittig.macros.annotation

import scala.annotation.experimental

// Will print the Tree to the console, when "sbt compile" is called.
@experimental
@printTree
case class Person(name: String, alter: Int)
