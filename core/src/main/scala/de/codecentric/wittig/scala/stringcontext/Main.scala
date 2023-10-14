package de.codecentric.wittig.scala.stringcontext

case class Point(x: Double, y: Double)

extension (sc: StringContext)
  def p(args: Double*): Point =
    val pts =
      sc.s(args*) // reuse the `s`-interpolator and then split on ','
        .split(",", 2)
        .map(_.toDoubleOption.getOrElse(0.0))
    Point(pts(0), pts(1))

@main
def run =
  println("Hallo Welt")
  println(p"-1, 3")
  val a = 4
  println(p"$a,r")
