package de.codecentric.wittig.scala.malle18

import cats.arrow.FunctionK
import cats.free.Free

sealed trait KeyValueInstr[A]
case class Get(key: String)             extends KeyValueInstr[Int]
case class Put(key: String, value: Int) extends KeyValueInstr[Unit]

object KeyValueInstr {
  type FreeKV[A] = Free[KeyValueInstr, A]

  def get(key: String): FreeKV[Int] = Free.inject(Get(key))

  def put(key: String, value: Int): FreeKV[Unit] = Free.inject(Put(key, value))
}

object Programs extends App {
  type Id[A] = A

  import KeyValueInstr._

  val program: Free[KeyValueInstr, (Int, Int)] = for {
    _ <- put("scala", 42)
    _ <- put("foo", 1)
    x <- get("scala")
    _ <- put("scala", 1337)
    y <- get("scala")
  } yield (x, y)

  private val interp: FunctionK[KeyValueInstr, Id] = {
    var keyValueStore = Map[String, Int]()

    new FunctionK[KeyValueInstr, Id] {
      override def apply[A](fa: KeyValueInstr[A]): Id[A] = fa match {
        case Get(k) =>
          keyValueStore.get(k).get
        case Put(k, v) =>
          keyValueStore = keyValueStore.updated(k, v)
          ()
      }
    }
  }

  println(program.foldMap(interp))
}
