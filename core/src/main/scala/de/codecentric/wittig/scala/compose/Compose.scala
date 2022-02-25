package de.codecentric.wittig.scala.compose

object Compose extends App:
  composeMethods()
  composeFunctions()

  def composeMethods() =
    def add1(x: Int)      = x + 1
    def add100(x: Int)    = x + 100
    def stringify(x: Int) = x.toString + "hallo"
    def upp(s: String)    = s.toUpperCase
    val all               = upp compose stringify compose add100 compose add1

    println(s"all: ${all(2)}")

  def composeFunctions() =
    val vadd1      = (x: Int) => x + 1
    val vadd100    = (x: Int) => x + 100
    val vstringify = (x: Int) => x.toString + "hallo"
    val vupp       = (s: String) => s.toUpperCase
    val vall       = vupp compose vstringify compose vadd100 compose vadd1

    println(s"vall: ${vall(2)}")
