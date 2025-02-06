package de.wittig.macros.rockthejvm.macros
import quoted.*

object JReflectionBasics:

  // instance.methodName(arg)
  inline def callMethodDynamically[A](instance: A, arg: Int, methodName: String): String =
    ${ callMethodDynamicallyImpl('instance, 'arg, 'methodName) }

  private def callMethodDynamicallyImpl[A: Type](instance: Expr[A], arg: Expr[Int], methodName: Expr[String])(using Quotes): Expr[String] =
    import quotes.reflect.*

    // Term = losely typed Expr = piece of an AST
    val term = instance.asTerm

    // insect Terms
    // Select = programmatic construction of a structure e.g. instance.methodName
    val maybeMethod = Select.unique(term, methodName.valueOrAbort)

    // Apply = programmatic construction of a structure e.g. instance.methodName(arg)
    val apply = Apply(maybeMethod, List(arg.asTerm))

    // after we build the expressions, we can turn it into the type we need
    apply.asExprOf[String]

  /** Generate Tuple with N fields of Type A */
  // createTuple[3, Int](42) => (42, 42, 42)
  transparent inline def createTuple[N <: Int, A](inline a: A) = ${ createTupleImpl[N, A]('a) }

  private def createTupleImpl[N: Type, A: Type](a: Expr[A])(using Quotes) =
    import quotes.reflect.*

    def buildTupleSimple(n: Int): Expr[Tuple] =
      if (n == 0) '{ EmptyTuple }
      else
        '{ $a *: ${ buildTupleSimple(n - 1) } }

    TypeRepr.of[N] match
      case ConstantType(IntConstant(n)) if n > 1 => buildTupleSimple(n)
      case _                                     => report.errorAndAbort("N must be a constant: " + Type.show[N])
