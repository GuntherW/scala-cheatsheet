package de.wittig.scala.scalacheck

import de.wittig.scala.scalacheck.helper.Person
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalacheck.ScalacheckShapeless._ // This is the "magic" import

object CaseClassViaShapeless extends Properties("WithShapeless") {
  property("person") = forAll { (p: Person) =>
    println(s"Person: $p")
    true
  }
}
