package de.codecentric.wittig.scala.typeclass

/**
 * @author gunther
 */
object FireExample {
  case class Alcohol(liters: Double)
  case class Water(liters: Double)

  case class Fire(heat: Double)

  trait Flammable[A] {
    def burn(fuel: A): Fire
  }

  implicit object AlcoholIsFlammable extends Flammable[Alcohol] {
    def burn(fuel: Alcohol) = Fire(120.0)
  }

  def setFire[T](fuel: T)(implicit f: Flammable[T]) = f.burn(fuel)

  setFire(Alcohol(1.0)) // ok
  //setFire(Water(1.0)) // FAIL

}
