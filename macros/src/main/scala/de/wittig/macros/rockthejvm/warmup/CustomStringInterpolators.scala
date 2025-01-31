package de.wittig.macros.rockthejvm.warmup

object CustomStringInterpolators extends App {

  private val pi = 3.14159

  // s-interpolator
  private val sInterpolator = s"The value of PI is appox ${pi + 0.000002}"

  // f-interpolator
  private val fInterpolator = f"The value of PI up to 3 sig digits is $pi%3.2f"

  // raw-interpolator
  private val rawInterpolator = raw"The value of pi is $pi\n This is not a new line"

  println(sInterpolator)
  println(fInterpolator)
  println(rawInterpolator)

  case class Person(name: String, age: Int)

  def string2Person(line: String): Person =
    val tokens = line.split(",")
    Person(tokens(0), tokens(1).toInt)

  extension (sc: StringContext)
    def pers(args: Any*): Person =
      val concat = sc.s(args*)
      string2Person(concat)

  val gunther = pers"gunther,123"
  println(gunther)

  val name     = "gunther2"
  val age      = 123
  val gunther2 = pers"$name,$age"
  println(gunther2)
}
