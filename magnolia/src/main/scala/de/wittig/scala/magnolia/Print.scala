package de.wittig.scala.magnolia

import de.wittig.scala.magnolia.Print.Typeclass
import magnolia1.*

trait Print[T]:
  extension (x: T) def print: String

object Print extends AutoDerivation[Print]:

  def join[T](ctx: CaseClass[Typeclass, T]): Print[T] = value =>
    ctx.params.map { param =>
      val label = param.label
      val wert  = param.typeclass.print(param.deref(value))
      val index = param.index
      s"$label[${Console.RED_B}$index${Console.RESET}]=$wert"
    }.mkString(s"${ctx.typeInfo.short}(", ",", ")")

  override def split[T](ctx: SealedTrait[Print, T]): Print[T] =
    value =>
      ctx.choose(value) { sub =>
        sub.typeclass.print(sub.cast(value))
      }

  given Print[Int]    = i => s"${Console.RED}$i${Console.RESET}"
  given Print[String] = i => s"${Console.BLUE}$i${Console.RESET}"
  given Print[Double] = i => s"${Console.YELLOW}$i${Console.RESET}"
