package de.codecentric.wittig.scala.scala213

object Main extends App {
  literalTypes()
  underScoresInNumerics()
  patternMatchingStrings()
  collectionZeugs()

  def literalTypes(): Unit = {
    def foo[T](t: T): t.type = t

    val eins: 1 = 1
    println(eins)
    println(foo(23))
  }

  def underScoresInNumerics(): Unit = {
    val a = 1_000_000
    val b = 1000000
    assert(a == b)
  }

  def patternMatchingStrings(): Unit =
    "Hallo Welt" match {
      case s"hallo $v" => println("klein")
      case s"Hallo $v" => println("groß")
      case _           => println("keines")
    }

  def collectionZeugs(): Unit = {
    val l = List(1, 2, 3)
    l.maxOption.foreach(println)  // maxOption ist neu. Für potentiell leere collections
    println(l.stepper.nextStep()) // Stepper als effizienter Iterator (mit "hasStep" und "nextStep")
    l.view.filter(_ < 3)

    val m = Map(1 -> "eins", 2 -> "zwei")
    //m.mapValues(_.capitalize)            // "mapValues" und "filterKeys" deprecated
    m.view.mapValues(_.capitalize).toMap // use .view.mapValues(A=>B).toMap instead
    println(m.view.filterKeys(_ < 2).mapValues(_.capitalize).toMap)
  }
}
