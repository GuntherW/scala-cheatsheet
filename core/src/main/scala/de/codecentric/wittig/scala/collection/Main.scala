package de.codecentric.wittig.scala.collection

/** Forgotten collection APIs
  */
object Main extends App:

  transpose()
  unfold()

  private def transpose(): Unit =
    println(List(Set(1, 2, 3), Set(4, 5, 6)).transpose.transpose)
    println(List("111000", "101010").transpose)

  private def unfold(): Unit =
    val unfolded = List.unfold(0) { i =>
      if i < 10 then Some(i, i + 1) else None
    }
    println(unfolded)

    val fibonacci = Iterator.unfold((BigInt(0), BigInt(1))) {
      case (x, y) => Some((x, (y, x + y)))
    }

    println(fibonacci.take(10).toList)
    println(fibonacci.drop(90).next)
