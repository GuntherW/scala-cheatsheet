//> using scala 2.13.18
//> using lib "ch.epfl.scala::scalafix-core:0.14.7"

import scalafix.v1._
import scala.meta._

class MyRule extends SyntacticRule("MyRule") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect { case t @ Term.Name("badName") =>
      Patch.replaceTree(t, "betterName")
    }.asPatch
  }
}
