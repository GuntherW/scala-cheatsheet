package de.wittig.zio.rockthejvm.effects
import zio.*

object ZIOApps:

  def main(args: Array[String]): Unit =
    val runtime        = Runtime.default
    given trace: Trace = Trace.empty
    Unsafe.unsafe { unsafe =>
      given u: Unsafe = unsafe

      println(runtime.unsafe.run(ZIO.succeed(42)))
    }

object BetterApp extends ZIOAppDefault:
  override def run = ZIO.succeed(42).debug

