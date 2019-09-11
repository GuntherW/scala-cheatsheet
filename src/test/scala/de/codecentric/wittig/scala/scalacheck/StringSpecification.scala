package de.codecentric.wittig.scala.scalacheck
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

object StringSpecification extends Properties("String") {

  property("startsWith") = forAll { (a: String, b: String) =>
    (a + b).startsWith(a)
  }

  property("concatenation") = forAll { (a: String, b: String) =>
    (a + b).length >= a.length && (a + b).length >= b.length
  }
  //Dieser Test würde fehlschlagen
  //  property("concatenate") = forAll { (a: String, b: String) =>
  //  (a + b).length > a.length && (a + b).length > b.length
  //  }

  property("substring") = forAll { (a: String, b: String, c: String) =>
    (a + b + c).substring(a.length, a.length + b.length) == b
  }

  //Mit eigenem Generator, der nur Zahlen zwischen 30 und 60 ausspuckt
  val ge = Gen.choose(30, 60)
  property("toInt") = forAll(ge) { (a: Int) =>
    a.toString().toInt == a
  }

  // Generator für ein Int-Tupel, bei dem der zweite Wert mindestens doppelt so hoch wie der erste Wert ist.
  val myGen = for {
    n <- Gen.choose(10, 20)
    m <- Gen.choose(2 * n, 500)
  } yield (n, m)
  property("int tuples") = forAll(myGen) { a: (Int, Int) =>
    (a._1 + 10) < a._2
  }

  //Generieren von geraden Zahlen auf unterschiedliche Weise.
  val evenInteger      = Arbitrary.arbitrary[Int] suchThat (_ % 2 == 0)
  val smallEvenInteger = Gen.choose(0, 200) suchThat (_       % 2 == 0)
  property("Gerade Ints") = forAll(smallEvenInteger, evenInteger) { (a: Int, b: Int) =>
    (a + b) % 2 == 0
  }

}
