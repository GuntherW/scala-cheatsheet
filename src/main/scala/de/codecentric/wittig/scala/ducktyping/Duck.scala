package de.codecentric.wittig.scala.ducktyping

import language.reflectiveCalls

object Duck extends App {
  def quaken(duck: { def quak(s: String): String }): Unit = {
    println(duck.quak("Quak"))
  }

  object BigDuck {
    def quak(s: String): String = {
      s.toUpperCase
    }
  }
  object SmallDuck {
    def quak(s: String): String = {
      s.toLowerCase
    }
  }
  object AnythingButADuck {
    def quak(s: String): String = {
      "I am different"
    }
  }

  quaken(BigDuck)
  quaken(SmallDuck)
  quaken(AnythingButADuck)
}
