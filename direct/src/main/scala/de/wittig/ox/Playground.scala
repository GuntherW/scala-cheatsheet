package de.wittig.ox

import ox.*
import ox.scheduling.*

import scala.concurrent.duration.*

object Playground extends App:

  def insert(i: Int) =
    sleep(2.seconds)
    println(s"insert ${i}")

  supervised:
    forkUser:
      repeat(RepeatConfig.immediate(5000))(insert(1))
    forkUser:
      repeat(RepeatConfig.immediate(5000))(insert(2))
