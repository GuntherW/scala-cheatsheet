package de.wittig.macros.rockthejvm.macros
import quoted.*

object NTreeMatching:

  inline def demoTreeMatching[A](inline value: A): Unit = ${ demoTreeMatchingImpl('value) }

  private def demoTreeMatchingImpl[A: Type](value: Expr[A])(using Quotes): Expr[Unit] =
    import quotes.reflect.*
    val term = value.asTerm

    println("################## start ###################")
    println(term.show(using Printer.TreeStructure))
    term match
      case Inlined(_, _, Apply(Ident(function), args)) =>
        println(s"Found an inlined block. function: $function with arguments: ${args.map(_.show)}")
      case _                                           =>
        println("Not an inlined block")
    println("################## end ###################")
    '{ () }
