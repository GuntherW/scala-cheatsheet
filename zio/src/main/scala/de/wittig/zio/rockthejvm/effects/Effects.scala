package de.wittig.zio.rockthejvm.effects

import zio.*
object Effects {

  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val another: ZIO[Any, Nothing, Int]       = ZIO.succeed(100)
  val failure: ZIO[Any, String, Nothing]    = ZIO.fail("Something went wrong")
  val supended: ZIO[Any, Throwable, Int]    = ZIO.suspend(meaningOfLife)

  // zip
  val zipped: ZIO[Any, Nothing, (Int, Int)] = meaningOfLife.zip(another)

  // zipWith
  val zippedWith: ZIO[Any, Nothing, Int] = meaningOfLife.zipWith(another)(_ * _)

  def sequenceTakeLast[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, B] =
    for
      _ <- zioa
      b <- ziob
    yield b

  def sequenceTakeLast_v2[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, B] =
    zioa *> ziob

  def sequenceTakeFirst[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, A] =
    for
      a <- zioa
      _ <- ziob
    yield a

  def sequenceTakeFirst_v2[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, A] =
    zioa <* ziob

  def runForever[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A]    =
    zio.flatMap(_ => runForever(zio))
  def runForever_v2[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    zio *> runForever(zio)

  def convert[R, E, A, B](zio: ZIO[R, E, A], value: B): ZIO[R, E, B]    =
    zio.map(_ => value)
  def convert_v2[R, E, A, B](zio: ZIO[R, E, A], value: B): ZIO[R, E, B] =
    zio.as(value)

  def asUnit[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, Unit]    =
    zio.map(_ => ())
  def asUnit_v2[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, Unit] =
    zio.unit

  def sumZIO(n: Int): UIO[Int] =
    if (n == 0) ZIO.succeed(0)
    else
      for
        current <- ZIO.succeed(n)
        that    <- sumZIO(n - 1)
      yield current + that

  def fibonacciZIO(n: Int): UIO[Int] =
    if (n <= 2) ZIO.succeed(1)
    else
      for
        last <- ZIO.suspendSucceed(fibonacciZIO(n - 1))
        prev <- fibonacciZIO(n - 2)
      yield last + prev

  def main(args: Array[String]): Unit =
    val runtime        = Runtime.default
    given trace: Trace = Trace.empty

    Unsafe.unsafe { implicit u =>

      val firstEffect  = ZIO.succeed {
        println("First Effect")
        Thread.sleep(1000)
        1
      }
      val secondEffect = ZIO.succeed {
        println("Second Effect")
        Thread.sleep(1000)
        2
      }
//      println(runtime.unsafe.run(sequenceTakeFirst(firstEffect, secondEffect)))
//      println(runtime.unsafe.run(sequenceTakeFirst_v2(firstEffect, secondEffect)))
//      println(runtime.unsafe.run(sequenceTakeLast(firstEffect, secondEffect)))
//      println(runtime.unsafe.run(sequenceTakeLast_v2(firstEffect, secondEffect)))
//      println(runtime.unsafe.run(runForever(firstEffect)))
//      println(runtime.unsafe.run(runForever_v2(firstEffect)))
//      println(runtime.unsafe.run(convert(firstEffect, 123)))
//      println(runtime.unsafe.run(convert_v2(firstEffect, 123)))
//      println(runtime.unsafe.run(asUnit(ZIO.succeed(println("Hallo Welt")))))
//      println(runtime.unsafe.run(asUnit_v2(ZIO.succeed(println("Hallo Welt")))))
//      println(runtime.unsafe.run(sumZIO(20000)))
      println(runtime.unsafe.run(fibonacciZIO(6)))
    }

}
