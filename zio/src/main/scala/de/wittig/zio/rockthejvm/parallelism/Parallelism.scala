package de.wittig.zio.rockthejvm.parallelism

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object Parallelism extends ZIOAppDefault {

  val meaningOfLife = ZIO.succeed(42)
  val favLang       = ZIO.succeed("Scala")
  val combined      = meaningOfLife.zip(favLang)

  // combine in parallel
  val combinedPar  = meaningOfLife.zipPar(favLang)
  val combinedPar1 = meaningOfLife.zipWithPar(favLang)((meaning, lang) => s"$lang $meaning")

  // collectAllPar
  val effects         = (1 to 10).map(i => ZIO.succeed(i).debugThread)
  val collectedValues = ZIO.collectAllPar(effects) // "traverse"

  // foreachPar
  val printlnParallel = ZIO.foreachPar((1 to 10).toList)(i => ZIO.succeed(println(i)))

  // reduceAllPar, mergeAllPar
  val sumPar  = ZIO.reduceAllPar(ZIO.succeed(0), effects)(_ + _)
  val sumPar2 = ZIO.mergeAllPar(effects)(0)(_ + _)

  def run = collectedValues.debugThread
}
