package com.example.copy

object Main extends App {

  case class Person(name: String, alter: Int, adresse: Adresse)
  case class Adresse(strasse: String, plz: Int)

  val p = Person("Henning", 3, Adresse("lkj", 444))
  val f = Person("Henning", 3, Adresse("ll", 444))

  val p2 = p.copy(adresse = p.adresse.copy(strasse = "lll"))

  println(p2)

}
