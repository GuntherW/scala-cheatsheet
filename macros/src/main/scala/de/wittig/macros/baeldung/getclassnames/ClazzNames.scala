package de.wittig.macros.baeldung.getclassnames

import scala.quoted.{Expr, Quotes, Type}

/** https://www.baeldung.com/scala/macros-scala-3
  */
object ClazzNames:

  inline def getType1[T](obj: T): String = ${ getType1Impl('obj) }
  inline def getType2[T](obj: T): String = ${ getType2Impl('obj) }

  private def getType1Impl[T: Type](obj: Expr[T])(using Quotes): Expr[String] = '{
    val o: T = $obj
    o.getClass.getSimpleName
  }

  private def getType2Impl[T](obj: Expr[T])(using t: Type[T])(using Quotes): Expr[String] = '{
    val o: t.Underlying = $obj
    o.getClass.getSimpleName
  }
