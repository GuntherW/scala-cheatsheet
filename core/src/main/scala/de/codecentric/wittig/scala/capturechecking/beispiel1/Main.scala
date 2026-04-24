package de.codecentric.wittig.scala.capturechecking.beispiel1
import language.experimental.captureChecking

/**
 * https://tanishiking.github.io/posts/introduction-to-scala-3s-capture-checking-and-separation-checking/
 */
class Counter:
  def inc(): Counter = {
    println("inc called")
    this
  }

@main def main(): Unit = {

  val c1: Counter^ = Counter()
  val c2: Counter^ = Counter()

  // Hier wird -> anstatt von => benutzt. A->B repräsentiert eine pure Funktion
  val increment:(() -> Unit)^{c1}  =
    () => c1.inc()
//    () => c2.inc() // (c2: Counter^) cannot be referenced here

  increment()
}
