package de.codecentric.wittig.scala.types.varianz

object ContravarianzApp extends App:

  class Animal
  class Dog(name: String) extends Animal

  trait Tierarzt[-T]:
    def heilen(animal: T): Boolean

  def holeTierarzt: Tierarzt[Dog] = new Tierarzt[Animal] {
    override def heilen(animal: Animal): Boolean =
      println(s"Tier ist geheilt")
      true
  }
  val dog                         = new Dog("Lassie")
  val arzt: Tierarzt[Dog]         = holeTierarzt

  arzt.heilen(dog)
