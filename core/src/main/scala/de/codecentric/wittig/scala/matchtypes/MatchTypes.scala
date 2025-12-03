package de.codecentric.wittig.scala.matchtypes

@main
def matchTypes(): Unit =

  /** Matching types */
  type ConstituentPartOf[A] = A match
    case BigInt  => Int
    case String  => Char
    case List[a] => a

  val aNumber: ConstituentPartOf[BigInt]         = 2
  val aChar: ConstituentPartOf[String]           = 'a'
  val anElement: ConstituentPartOf[List[String]] = "Hallo"

  /** Folgende drei Methoden haben das gleich Muster: */
  def lastDigitOf(number: BigInt): Int =
    (number % 10).toInt

  def lastCharOf(string: String): Char =
    if (string.isEmpty) throw new NoSuchElementException
    else string.charAt(string.length - 1)

  /** Und können zusammengefasst werden: */
  def lastPartOf[A](a: A): ConstituentPartOf[A] =
    a match
      case n: BigInt  => (n % 10).toInt
      case s: String  =>
        if (s.isEmpty) throw new NoSuchElementException
        else s.charAt(s.length - 1)
      case l: List[_] =>
        if (l.isEmpty) throw new NoSuchElementException
        else l.last

  val a: Char = lastPartOf("Scala")
  assert(lastPartOf("Scala") == 'a')
  assert(lastPartOf(BigInt("13")) == 3)
  assert(lastPartOf(List(1, 2, 3)) == 3)

  /** Recursive Matching types */
  type LowestLevelPartOf[A] = A match
    case List[a] => LowestLevelPartOf[a]
    case _       => A

  val lastPartOfNestedList: LowestLevelPartOf[List[List[List[Int]]]] = 10
