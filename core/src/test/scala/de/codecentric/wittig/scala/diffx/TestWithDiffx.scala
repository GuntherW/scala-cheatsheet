package de.codecentric.wittig.scala.diffx

import org.scalatest.funsuite.AnyFunSuite
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.generic.auto._
import org.scalatest.matchers.should.Matchers._

class TestWithDiffx extends AnyFunSuite {

  sealed trait Person
  case class Manager(name: String, age: Int)                                           extends Person
  case class Employee(manager: Manager, favNumbers: List[Int], friend: Option[Person]) extends Person

  test("fail with good diff") {

    val ist  = Employee(Manager("Hans", 6), List(1234), Some(Employee(Manager("Hans", 6), List(1234), None)))
    val soll = Employee(Manager("Hans", 5), List(123, 1234), Some(Manager("Peter", 5)))

    ist should matchTo(soll)
  }
}
