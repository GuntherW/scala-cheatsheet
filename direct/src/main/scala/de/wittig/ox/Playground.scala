package de.wittig.ox

import ox.*
import ox.scheduling.*

import scala.concurrent.duration.*

object Playground extends App:

  def insert(i: Int) =
    sleep(2.seconds)
    println(s"insert $i")

  supervised:
    forkUser:
      repeat(Schedule.immediate)(insert(1))
    forkUser:
      repeat(Schedule.immediate)(insert(2))
