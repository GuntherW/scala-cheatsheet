package de.codecentric.wittig.scala.cat.effectCps

import cats.effect.{IO, IOApp}
import cats.effect.cps.*

import scala.concurrent.duration.*

object Main extends IOApp.Simple:

  private val io1 = IO.sleep(1.second).as(1)
  private val io2 = IO.sleep(1.second).as(2)
  private val io3 = IO.sleep(1.second).as(3)

  private val program =
    async[IO] {
      io1.await match
        case 1 => 1 + io2.await
        case i => i + io3.await
    }

  override def run = program.debug("result").void
