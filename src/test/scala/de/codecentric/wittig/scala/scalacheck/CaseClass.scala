package de.codecentric.wittig.scala.scalacheck

import org.scalacheck._
import Gen._
import Arbitrary._
import org.scalacheck.Prop.forAll

/**
  * @author gunther
  */
object CaseClass extends Properties("Person") {

  case class Person(name: String, age: Int) {
    def birthday: Person = copy(age = age + 1)
  }

  // Diese impliciter Arbitrary[Person] kann auch durch shapeless-scalacheck automatisch erzeugt werden. Jedoch nur mit Standardwerten für die einfachen Typen (String und Int)
  implicit val arbPerson: Arbitrary[Person] = Arbitrary {
    for {
      name <- arbitrary[String]
      age <- Gen.choose(0, 300)
    } yield Person(name, age)
  }

  property("birthday makes people older") = forAll { (person: Person) =>
    person.birthday.age > person.age
  }

}
