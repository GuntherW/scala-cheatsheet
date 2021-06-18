import magnolia.*

trait Print[T] {
  extension (x: T) def print: String
}

object Print extends AutoDerivation[Print] {

  def join[T](ctx: CaseClass[Typeclass, T]): Print[T] = value =>
    ctx.params.map { param =>
      param.typeclass.print(param.deref(value))
    }.mkString(s"${ctx.typeInfo.short}(", ",", ")")

  override def split[T](ctx: SealedTrait[Print, T]): Print[T] = {
    value =>
      ctx.choose(value) { sub =>
        sub.typeclass.print(sub.cast(value))
      }
  }

  //   given Print[Int] = _.toString
  implicit val intPrint: Print[Int] = _.toString
}