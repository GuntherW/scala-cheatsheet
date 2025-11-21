package de.codecentric.wittig.scala.equality

case class One(i: Int)
case class OneE(i: Int) derives CanEqual
case class Two(i: Int)
case class TwoE(i: Int) derives CanEqual

@main
def main(): Unit =

  val one  = One(1)
  val oneE = OneE(1)
  val two  = Two(1)
  val twoE = TwoE(1)

  withStrictEquality
  withoutStrictEquality

  def withoutStrictEquality =
    println(oneE == oneE)
    println(one == one)
    println(one == two)
//    println(oneE == twoE)

  def withStrictEquality =
    import scala.language.strictEquality // or "-language:strictEquality"
    println(oneE == oneE)
//    println(one == one)
//    println(one == two)
//    println(oneE == twoE)
