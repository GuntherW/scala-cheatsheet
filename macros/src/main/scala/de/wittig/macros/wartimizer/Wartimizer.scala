package de.wittig.macros.wartimizer
import quoted.*

object Wartimizer {
  inline def wartimize[A](inline w: Wartimization, inline ws: Wartimization*)(inline block: A): A = ${ wartimizeImpl('w, 'ws, 'block) }

  private def wartimizeImpl[A: Type](
      w: Expr[Wartimization],
      ws: Expr[Seq[Wartimization]],
      block: Expr[A]
  )(using q: Quotes): Expr[A] =
    import q.reflect.*

    // get Wartimization instances
    val wartimizations = w.valueOrAbort +: ws.valueOrAbort

    // get tree maps
    val treeMaps = wartimizations.map(_.treeMap)

    val finalTree = treeMaps.foldLeft(block.asTerm) { (currTree: Tree, treeMap) =>
      treeMap.transformTree(currTree)(Symbol.spliceOwner)
    }

    finalTree.asExprOf[A]
}
