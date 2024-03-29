package de.wittig.scala.scalacheck

import de.wittig.scala.scalacheck.helper.{Person, Season}
import org.scalacheck.Arbitrary.*
import org.scalacheck.Prop.forAll
import org.scalacheck.*
import scala.language.adhocExtensions

/** @author
  *   gunther
  */
object CaseClass extends Properties("Person"):
  // Diese implizite Arbitrary[Person] kann auch durch shapeless-scalacheck automatisch erzeugt werden. Jedoch nur mit Standardwerten für die einfachen Typen (String und Int)
  given Arbitrary[Person] = Arbitrary {
    for
      firstName <- Gen.alphaStr
      lastName  <- Arbitrary.arbitrary[String] // We can get the "default" Gen via Arbitrary.arbitrary[T].
      age       <- Gen.chooseNum(0, 123)
      season    <- Gen.oneOf(Season.Spring, Season.Summer, Season.Autumn, Season.Winter)
    yield Person(firstName, lastName, age, season)
  }

  property("birthday makes people older") = forAll { (person: Person) =>
    person.birthday.age > person.age
  }
