package de.codecentric.wittig.scala.option

/** Thanks to Marconi Lanna! https://skillsmatter.com/skillscasts/7040-lightning-talks-4
  */
object UseOption extends App:

  val p: String => Boolean = _.isEmpty
  val f: String => String  = _.toUpperCase

  test(
    {
      case Some(x) => Some(f(x))
      case None    => None
    },
    _.map(f)
  )

  test(
    {
      case Some(x) => x
      case None    => "b"
    },
    _.getOrElse("b")
  )

  test(
    {
      case Some(x) => f(x)
      case None    => "b"
    },
    _.fold("b")(f)
  )

  test(
    {
      case Some(_) => true
      case None    => false
    },
    _.isDefined
  )

  test(
    {
      case Some(_) => false
      case None    => true
    },
    _.isEmpty
  )

  test(
    {
      case Some(_) => 1
      case None    => 0
    },
    _.size
  )

  test(
    {
      case Some(x) => Some(x)
      case None    => Some("b")
    },
    _.orElse(Some("b"))
  )

  test(
    {
      case Some(x) => x
      case None    => null
    },
    _.orNull
  ) // maybe for compability reasons for java libs

  test(
    {
      case Some(x) if p(x) => Some(x)
      case _               => None
    },
    _.filter(p)
  )

  test(
    {
      case Some(x) if !p(x) => Some(x)
      case _                => None
    },
    _.filterNot(p)
  )

  test(
    {
      case Some(x) => x == "c"
      case None    => false
    },
    _.contains("c")
  )

  test(
    {
      case Some(x) => x.forall(_.isUpper)
      case None    => false
    },
    _.exists(_.forall(_.isUpper))
  )

  test(
    {
      case Some(x) => p(x)
      case None    => false
    },
    _.exists(p)
  )

  test(
    {
      case Some(x) => p(x)
      case None    => true
    },
    _.forall(p)
  )

  println("All Tests passed")

  def test[A](g: Option[String] => A, h: Option[String] => A): Unit =
    val someString      = Some("a")
    val someEmptyString = Some("")
    List(someString, someEmptyString, None)
      .foreach { opt =>
        val aa = g(opt)
        val bb = h(opt)
        assert(aa == bb)
      }
