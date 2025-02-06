package de.wittig.macros.rockthejvm.macros
import quoted.*

object KStructuralTypes:

  class Record(fields: Map[String, Any]) extends Selectable:
    def selectDynamic(name: String): Any = fields(name)
  object Record:

    // Record(fields.toMap) { val name:String = "Alice", val age:Int = 30, val favLanguage:String = "Scala" }
    transparent inline def make(inline fields: (String, Any)*): Record = ${ makeImpl('fields) }

    private def makeImpl(fields: Expr[Seq[(String, Any)]])(using Quotes): Expr[Record] =
      import quotes.reflect.*

      val parentType = TypeRepr.of[Record]

      // example refinement with a field jediLevel:String
      val refinement = Refinement(parentType, "jediLevel", TypeRepr.of[String]) // type refinement of Record { val jediLevel: String }

      val resultType = fields match
        case Varargs(list) =>
          list.foldLeft(parentType) { (refinement, field) =>
            field match
              case '{ ($nameExpr: String, $value: a) }     =>
                // build a new refinement
                val name = nameExpr.value.getOrElse(badFieldError(nameExpr))
                val tpe  = TypeRepr.of[a]
                Refinement(refinement, name, tpe)
              case '{ ($nameExpr: String) -> ($value: a) } =>
                // build a new refinement
                val name = nameExpr.value.getOrElse(badFieldError(nameExpr))
                val tpe  = TypeRepr.of[a]
                Refinement(refinement, name, tpe)
          }

        case _ => report.errorAndAbort(s"Cannot build refinement type for ${fields.show}")

      // resultType:TypeRepr
      // I need the complete type Record { val ...}
      resultType.asType match
        case '[
            type r <: Record; r] => '{ Record($fields.toMap).asInstanceOf[r] }

  def badFieldError(expr: Expr[?])(using Quotes) =
    import quotes.reflect.*
    report.errorAndAbort(s"Only literal values and compile-time computable expressions allowed.")
