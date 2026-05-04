package de.codecentric.wittig.scala.tuple

@main
def mapping() = {

  val tup = (1, 2, 3)
  println(tup)

  // Won't compile — t is not known to be Int:
  // tup.map([t] => (x: t) => x + 1)

  val tup2 = tup.map[[X] =>> Int]([t] => (x: t) => x.asInstanceOf[Int] + 1) // Returns (2, 3, 4), but nothing stops you from calling this on ("a", "b", "c")
  println(tup2)

}
