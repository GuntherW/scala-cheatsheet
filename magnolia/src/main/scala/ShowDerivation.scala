import magnolia._
import scala.language.experimental.macros

object ShowDerivation {
  type Typeclass[T] = Show[T]

  def combine[T](ctx: CaseClass[Show, T]): Show[T] = new Show[T] {
    def show(value: T): String = ctx.parameters
      .map { p =>
        s"${p.label}=${p.typeclass.show(p.dereference(value))}"
      }
      .mkString(ctx.typeName.short + "{", ",", "}")
  }

  def dispatch[T](ctx: SealedTrait[Show, T]): Show[T] =
    new Show[T] {
      def show(value: T): String = ctx.dispatch(value) { sub =>
        sub.typeclass.show(sub.cast(value))
      }
    }

  implicit def gen[T]: Show[T] = macro Magnolia.gen[T]

  implicit class ShowOps[A](value: A) {
    def show(implicit p: Show[A]) = p.show(value)
  }
}
