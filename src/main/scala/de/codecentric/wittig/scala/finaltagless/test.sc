import de.codecentric.wittig.scala.finaltagless.Expr

def program[A](expr:Expr[A]):A = {
  import expr._
  add(
    add(
      zero,
      one
    ),one
  )
}

object IntExpr extends Expr[Int]{
  override def add(a: Int, b: Int): Int = a+b
  override def zero = 0
  override def one = 1
}

program(IntExpr)