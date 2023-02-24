package de.wittig.metaprogramming.conversion

object Main extends App {

//  quote : transform code to data
//  splice (or eval) : evaluate data as code

  import scala.quoted.* // imports Quotes, Expr

  // inspectImpl goes from AST to AST as a normal Scala function
  def inspectImpl(x: Expr[Any])(using Quotes): Expr[Any] =
    println(x.show)
    x

  //  converting code into AST and invoking inspectImpl
  inline def inspect(inline x: Any): Any = ${ inspectImpl('x) }

}
