package de.wittig

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class WordSpecScalaTest extends AnyWordSpecLike with Matchers {

  case class Person(name: String, color: String)

  "Person" when {
    "retrieving name" should {
      "return name" in {
        val p = Person("Hans", "red")
        p.name shouldBe "Hans"
      }
    }
  }
}
