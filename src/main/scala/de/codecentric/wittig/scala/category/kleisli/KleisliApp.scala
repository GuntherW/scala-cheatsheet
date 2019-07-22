package de.codecentric.wittig.scala.category.kleisli

/**
  *
  * http://www.leonardoborges.com/writings/2014/06/17/functional-composition-with-monads-kleisli-functors/
  */
object KleisliApp extends App {
  import scalaz._
  import Scalaz._
  import scalaz.Kleisli._

  case class Make(id: Int, name: String)
  case class Part(id: Int, name: String)

  // "normaler" compose
  einfach

  //  compose von Option
  mitOption

  // compose von Option mit einem Kleisli Arrow
  mitOptionKleisli

  def einfach() = {

    val make: Int => Make = (_) => Make(1, "Suzuki")
    val parts: Make => List[Part] = {
      case Make(1, _) => List(Part(1, "Gear Box"), Part(2, "cable"))
    }

    val f = parts compose make
    // val f = make andThen parts

    println(f(2))
  }

  def mitOption() = {
    val make  = (x: Int) => (x == 1).option(Make(1, "Suzuki"))
    val parts = (x: Make) => (x.id == 1).option(NonEmptyList(Part(1, s"Gear Box $x"), Part(2, "Clutch cable")))

    println(make(1))

    val f2 = (x: Int) =>
      for {
        m <- make(x)
        p <- parts(m)
      } yield p

    println(f2(1))
  }

  def mitOptionKleisli() = {
    val make  = (x: Int) => (x == 1).option(Make(1, "Suzuki"))
    val parts = (x: Make) => (x.id == 1).option(NonEmptyList(Part(1, s"Gear Box $x"), Part(2, "Clutch cable")))

    //A Kleisli arrow is simply a wrapper for a function of type A => F[B].
    val a: ReaderT[Option, Make, NonEmptyList[Part]] = kleisli(parts)
    println(a)

    val f3 = kleisli(parts) <==< make
    // same as   kleisli(parts0) composeK make
    println(f3(1))
  }
}
