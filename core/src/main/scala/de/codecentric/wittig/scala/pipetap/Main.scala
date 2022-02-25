package de.codecentric.wittig.scala.pipetap

import scala.util.chaining.*

object Main extends App:

  def plus1(i: Int)  = i + 1
  def double(i: Int) = i * 2
  def square(i: Int) = i * i

  // pipe ist wie das Unix "|"
  val x: Int   = 1.pipe(plus1).pipe(double)               // Kein Seiteneffekt, gibt Int zurück
  val x1: Unit = 1.pipe(plus1).pipe(double).pipe(println) // Seiteneffekt, aber gibt Unit zurück

  // tap ist eher wie das Unix "tee".
  // Kann einen Seiteneffekt auslösen und gibt das ursprüngliche Objekt zurück.
  val x2: Int = 1.pipe(plus1).pipe(double).tap(println) // Seiteneffekt und gibt Int zurück
