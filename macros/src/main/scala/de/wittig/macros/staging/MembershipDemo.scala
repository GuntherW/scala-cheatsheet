package de.wittig.macros.staging

import scala.quoted.*
import scala.quoted.staging.Compiler

/** Staging demo: generates a membership predicate from values only known at runtime.
  *
  * Run with: sbt --client "macros/runMain de.wittig.macros.staging.runMembershipDemo 1 3 5"
  */
object MembershipDemo:

  given Compiler = Compiler.make(getClass.getClassLoader)

  /** Generates `i => i == 1 || i == 3 || i == 5` for a list of values only known at runtime (e.g. read from config).
    *
    * The generated code contains the values as literals, so there is no list lookup when the function is called.
    */
  def membership(allowed: List[Int])(using Quotes): Expr[Int => Boolean] =
    def anyEquals(i: Expr[Int], values: List[Int]): Expr[Boolean] =
      values match
        case Nil          => Expr(false)
        case head :: tail => '{ $i == ${ Expr(head) } || ${ anyEquals(i, tail) } }
    '{ (i: Int) => ${ anyEquals('i, allowed) } }

  def run(args: List[String]): Unit =
    val allowed = if args.isEmpty then List(1, 3, 5) else args.map(_.toInt) // values known only at runtime

    println(s"generated code: ${staging.withQuotes(membership(allowed).show)}")

    val isAllowed = staging.run(membership(allowed))
    println(s"allowed = $allowed")
    println("Beispiel:")
    println(s"isAllowed(3) = ${isAllowed(3)}")
    println(s"isAllowed(4) = ${isAllowed(4)}")

@main
def runMembershipDemo(args: String*): Unit = MembershipDemo.run(args.toList)
