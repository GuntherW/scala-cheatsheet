package de.wittig.macros.rockthejvm.macros
import quoted.*

object QTreeMappings:

//   inline def transformCode[A](inline code: A): A = ${ transformCodeImpl('code) }

//   private def transformCodeImpl[A: Type](code: Expr[A])(using Quotes): Expr[A] =
//     import quotes.reflect.*

//     val treeMap = new TreeMap {
//       // TreeMap has various transformation methods, we can override
//       override def transformTerm(tree: Term)(owner: Symbol): Term = {
//         println("-------- start Term ------------")
//         println(tree.show(using Printer.TreeStructure))
//         println(tree.show(using Printer.TreeShortCode))
//         println("-------- end Term    -----------")
//         super.transformTerm(tree)(owner) // leave the Tree untouched
//       }

//       override def transformStatement(tree: Statement)(owner: Symbol): Statement = {
//         println("--------- start Statement -----------")
//         println(tree.show(using Printer.TreeStructure))
//         println(tree.show(using Printer.TreeShortCode))
//         println("--------- end Statement   -----------")
//         super.transformStatement(tree)(owner) // leave the Tree untouched
//       }
//     }

//     treeMap.transformTerm(code.asTerm)(Symbol.spliceOwner).asExprOf[A]

  inline def flipBooleans[A](inline code: A): A = ${ flipBooleansImpl('code) }

  private def flipBooleansImpl[A: Type](code: Expr[A])(using Quotes): Expr[A] =
    import quotes.reflect.*
    val treeMap = new TreeMap {
      override def transformTerm(tree: Term)(owner: Symbol): Term =
        tree match
          case Literal(BooleanConstant(value)) => Literal(BooleanConstant(!value))
          case _                               => super.transformTerm(tree)(owner) // recurse until we get to a "leaf" node

      override def transformStatement(tree: Statement)(owner: Symbol): Statement =
        tree match
          case vd @ ValDef(_, typeTree, Some(rhs)) if typeTree.tpe =:= TypeRepr.of[Boolean] =>
            given Quotes = vd.symbol.asQuotes
            val newRhs   = '{ !${ rhs.asExprOf[Boolean] } }.asTerm
            ValDef(vd.symbol, Some(newRhs))

          case dd @ DefDef(_, params, typeTree, Some(rhs)) if typeTree.tpe =:= TypeRepr.of[Boolean] =>
            given Quotes = dd.symbol.asQuotes
            val newRhs   = '{ !${ rhs.asExprOf[Boolean] } }.asTerm
            DefDef(dd.symbol, _ => Some(newRhs))

          case _ => super.transformStatement(tree)(owner)

    }
    treeMap.transformTerm(code.asTerm)(Symbol.spliceOwner).asExprOf[A]
