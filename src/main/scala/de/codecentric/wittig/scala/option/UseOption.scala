package de.codecentric.wittig.scala.option

/**
  * Thanks to Marconi Lanna!
  * https://skillsmatter.com/skillscasts/7040-lightning-talks-4
  *
  */
object UseOption extends App {

  type Opt = Option[String]
  val a: Opt = Some("a")
  val empty: Opt = Some("")
  val none: Opt = None
  val p: String => Boolean = _.isEmpty
  val f: String => String = _.toUpperCase

  test(
    _ match {
      case Some(a) => Some(f(a))
      case None    => None
    },
    _.map(f))

  test(
    _ match {
      case Some(a) => a
      case None    => "b"
    },
    _.getOrElse("b"))

  test(
    _ match {
      case Some(a) => f(a)
      case None    => "b"
    },
    _.fold("b")(f))

  test(
    _ match {
      case Some(a) => true
      case None    => false
    },
    _.isDefined)

  test(
    _ match {
      case Some(a) => false
      case None    => true
    },
    _.isEmpty)

  test(
    _ match {
      case Some(a) => 1
      case None    => 0
    },
    _.size)

  test(
    _ match {
      case Some(a) => Some(a)
      case None    => Some("b")
    },
    _.orElse(Some("b")))

  test(
    _ match {
      case Some(a) => a
      case None    => null
    },
    _.orNull) // maybe for compability reasons for java libs

  test(
    _ match {
      case Some(a) if p(a) => Some(a)
      case _               => None
    },
    _.filter(p))

  test(
    _ match {
      case Some(a) if !p(a) => Some(a)
      case _                => None
    },
    _.filterNot(p))

  test(
    _ match {
      case Some(a) => a == "c"
      case None    => false
    },
    _.contains("c"))

  println("All Tests passed")

  def test[A](g: Opt => A, h: Opt => A): Unit = {
    import scala.io.AnsiColor.{ GREEN, RED }
    val all = Seq(a, empty, none)
    all.foreach { opt =>
      val aa = g(opt)
      val bb = h(opt)
      assert(aa == bb)
      //      if (aa == bb)
      //        println(f"$GREEN OK: $opt%-7s -> $aa == $bb")
      //      else
      //        println(f"$RED Bad: $opt%-7s -> $aa != $bb")
    }
  }
}
