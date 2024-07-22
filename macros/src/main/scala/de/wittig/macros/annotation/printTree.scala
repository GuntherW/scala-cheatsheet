package de.wittig.macros.annotation

import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted.Quotes

@experimental
class printTree extends MacroAnnotation {
  override def transform(using q: Quotes)(tree: q.reflect.Definition): List[q.reflect.Definition] =
    import q.reflect.{*, given}

    println("print tree: " + tree.show(using Printer.TreeStructure))
    List(tree)
}
