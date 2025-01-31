package de.wittig.macros.rockthejvm.warmup

object MatchTypes extends App:

  def lastDigitOf(number: BigInt): Int = (number % 10).toInt

  def lastCharOf(string: String): Char =
    if (string.isEmpty) throw new NoSuchElementException
    else string.charAt(string.length - 1)

  def lastCharOf[A](list: List[A]): A =
    if (list.isEmpty) throw new NoSuchElementException
    else list.last

  type ConstituentPartOf[A] = A match {
    case BigInt  => Int
    case String  => Char
    case List[a] => a
  }

  val aNumber: ConstituentPartOf[BigInt]         = 2
  val aChar: ConstituentPartOf[String]           = 'a'
  val anElement: ConstituentPartOf[List[String]] = "Hallo"

  def lastPartOf[A](a: A): ConstituentPartOf[A] = a match
    case number: BigInt => (number % 10).toInt
    case string: String => if (string.isEmpty) throw new NoSuchElementException
      else string.charAt(string.length - 1)
    case list: List[_]  => if (list.isEmpty) throw new NoSuchElementException
      else list.last

  val lastPartOfString = lastCharOf("Scala") // 'a'
