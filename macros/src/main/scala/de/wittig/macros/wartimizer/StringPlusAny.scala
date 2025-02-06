package de.wittig.macros.wartimizer

import scala.quoted.*

// Wartimization that doesn't compile, if "string"+ something, if something is not a string
object StringPlusAny extends Wartimization {

  override def treeMap(using q: Quotes): q.reflect.TreeMap =
    import q.reflect.*

    new q.reflect.TreeMap {
      override def transformTerm(tree: Term)(owner: Symbol): Term =
        // this term must be an Expr
        // this eprx must be a String + something else
        if (!tree.isExpr)
          super.transformTerm(tree)(owner)
        else tree.asExpr match
          case '{ ($lhs: String) + ($rhs: t) } if !(TypeRepr.of[t] <:< TypeRepr.of[String]) =>
            report.errorAndAbort(s"Adding String to anythin else is forbidden", tree.pos)
          case _                                                                            => super.transformTerm(tree)(owner)
    }

}
