package de.codecentric.wittig.scala.jfr

import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

object JfrMain1 extends App:

  println("Hallo wlet")
  val event = new GreetingEvent
  event.begin()
  event.message = "Hallo Welt"
  event.counter = 123
  event.commit()

  @Label("mein greeting event")
  class GreetingEvent extends Event {

    @Label("greeting message")
    var message: String = ""

    @Label("greeting counter")
    var counter: Int = 0
  }
