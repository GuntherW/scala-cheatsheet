package de.codecentric.wittig.scala.stringcontext

case class Point(x: Double, y: Double)
case class Person(name: String, age: Int)

// Erstellen eines eigenen StringInterpolators
extension (sc: StringContext)

  // 1. Beispiel
  def p(args: Double*): Point =
    val pts = sc.s(args*) // reuse the `s`-interpolator and then split on ','
      .split(",", 2)
      .map(_.toDoubleOption.getOrElse(0.0))
    Point(pts(0), pts(1))

  // 2. Beispiel
  def pers(args: Any*): Person =
    val concat = sc.s(args*)
    val tokens = concat.split(",")
    Person(tokens(0), tokens(1).toInt)

@main
def run: Unit =
  val s = "Gun"
  val a = 4
  println(p"-1, 3")
  println(p"$a,r")

  println(pers"Gunther,123")
  println(pers"$s,$a")
