package de.wittig.macros.langton.school

import scala.quoted.*

object MacroSimple:

  // Macro definitions must be prefixed with the inline keyword.
  inline def blub: Unit = ${ blubImpl }

  // Macro implementation. (with using Quoutes). Returning Expr
  def blubImpl(using Quotes): Expr[Unit] = '{ () }
