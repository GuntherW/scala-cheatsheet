package de.codecentric.wittig.scala.malle18

trait KeyValueTagless {
  def get(key: String): Int
  def put(key: String, value: Int): Unit
}

object Tagless extends App {
  def program(implicit KVT: KeyValueTagless) = {
    import KVT._

    put("scala", 42)
    val scalaValue = get("scala")
    put("scala", 1337)
    val scalaValue2 = get("scala")

    println(scalaValue -> scalaValue2)
  }

  implicit val inMemory: KeyValueTagless = {
    var map = Map[String, Int]()

    new KeyValueTagless {
      override def get(key: String): Int = map.get(key).get

      override def put(key: String, value: Int): Unit = {
        map = map.updated(key, value)
      }
    }
  }

  def inMemoryOpt(previous: String)(implicit keyValueTagless: KeyValueTagless): KeyValueTagless = new KeyValueTagless {
    override def get(key: String): Int =
      if (previous == "put") {
        ???
      } else {
        keyValueTagless.get(key)
      }

    override def put(key: String, value: Int): Unit =
      if (previous == "put") {
        ???
      } else {
        keyValueTagless.put(key, value)
      }
  }

  println(program)
}
