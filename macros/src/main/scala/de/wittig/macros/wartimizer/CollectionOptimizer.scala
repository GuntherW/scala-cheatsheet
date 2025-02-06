package de.wittig.macros.wartimizer
import quoted.*

object CollectionOptimizer extends Wartimization {
  override def treeMap(using q: Quotes): q.reflect.TreeMap =
    import q.reflect.*

    new q.reflect.TreeMap {
      override def transformTerm(tree: Term)(owner: Symbol): Term =
        val maybeExpr            = Option.when(tree.isExpr)(tree).map(_.asExpr)
        val maybeTransformedExpr = maybeExpr.collect:
          case '{ ($x: collection.Map[k, v]).get($key).getOrElse($value) } =>
            '{ $x.getOrElse($key, $value) }
          case '{ ($x: collection.Iterable[t]).map[t2]($f).map[t3]($g) }   =>
            '{ $x.map(a => $g($f(a))) }
          case '{ ($x: collection.Iterable[t]).filter($f).headOption }     =>
            '{ $x.find($f) }

        maybeTransformedExpr
          .map(_.asTerm)
          .map(term => transformTerm(term)(owner))
          .getOrElse(super.transformTerm(tree)(owner))

    }
}
