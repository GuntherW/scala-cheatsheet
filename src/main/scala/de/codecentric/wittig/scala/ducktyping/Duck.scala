package de.codecentric.wittig.scala.ducktyping

object Duck extends App {

  def quaken(duck: { def quak(s: String): String }) = {
    println(duck.quak("Quak"))
  }

  object BigDuck {
    def quak(s: String) = {
      s.toUpperCase
    }
  }
  object SmallDuck {
    def quak(s: String) = {
      s.toLowerCase
    }
  }
  object AnythingButADuck {
    def quak(s: String) = {
      "I am different"
    }
  }

  quaken(BigDuck)
  quaken(SmallDuck)
  quaken(AnythingButADuck)
}
