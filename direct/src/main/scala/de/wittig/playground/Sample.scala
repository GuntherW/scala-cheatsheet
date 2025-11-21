package de.wittig.playground

import scala.util.Random

// A `Sample[A]` is a description of an effect that, when run, will generate a
// values of type `A` possibly using a random number generator
type Sample[A] = Random ?=> A

object Sample:
  // This runs a `Sample[A]` producing a value of type `A`. By default it uses
  // the global random number generator, but the user can pass in a different
  // generator as the first argument.
  def run[A](sample: Sample[A])(using random: Random = Random): A = sample

  // Utility to use inside a `Sample[A]` to produce a random `Int`
  def int(using r: Random): Int = r.nextInt()

  // Utility to use inside a `Sample[A]` to produce a random `Double`
  def double(using r: Random): Double = r.nextDouble()

  // Constructs a `Sample[A]`.
  inline def apply[A](inline body: Random ?=> A): Sample[A] = body

@main
def testBoth(): Unit =

  val printSample: (Console, Random) ?=> Unit =
    Print {
      val i = Sample(Sample.int)
      Print.println(i)
    }.prefix(Print.print("> ").red)

  Print.run(Sample.run(printSample))
