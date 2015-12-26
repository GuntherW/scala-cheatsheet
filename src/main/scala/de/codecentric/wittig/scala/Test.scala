package de.codecentric.wittig.scala

object Test extends App {

  println("hallo")

  val l = List(1, 2, 3, 4)
  println(l.map { x => x.toString })
  println(l.view.map { x => x.toString }.foreach(print))
  println(l.toStream.map { x => x.toString }.foreach(print))

  val s = Stream(1, 2, 3, 4)

  println(s.map(_.toString).take(3).toList)
}
