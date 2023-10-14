package de.wittig.macros.baeldung

import scala.quoted.{Expr, Quotes, Type}

/** https://www.baeldung.com/scala/macros-scala-3
  */
object ClazzNames:

  def getType1[T](obj: Expr[T])(using Type[T])(using Quotes): Expr[String] = '{
    val o: T = $obj
    o.getClass.getSimpleName
  }

  def getType2[T](obj: Expr[T])(using t: Type[T])(using Quotes): Expr[String] = '{
    val o: t.Underlying = $obj
    o.getClass.getSimpleName
  }

  inline def getTypeMacro1[T](obj: T): String = ${ getType1('obj) }
  inline def getTypeMacro2[T](obj: T): String = ${ getType2('obj) }
