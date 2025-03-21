package de.wittig.ox

import ox.*
import ox.channels.*

@main
def main() =

  val c = Channel.bufferedDefault[String]
  supervised:
    fork:
      c.send("Hello")
      c.send("World")
      c.done()

    fork:
      val r1 = c.receive()
      println(s"received: $r1")
      val r2 = c.receive()
      println(s"received: $r2")
      val r3 = c.receive()
      println(s"received: $r3")
