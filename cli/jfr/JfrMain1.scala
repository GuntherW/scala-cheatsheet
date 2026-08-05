package de.codecentric.wittig.scala.jfr

import jdk.jfr.{Event, Label}

@Label("mein greeting event")
class GreetingEvent extends Event {

  @Label("greeting message")
  var message: String = ""

  @Label("greeting counter")
  var counter: Int = 0
}

@main
def jfrMain1(): Unit =

  println("Hallo wlet")
  val event = new GreetingEvent
  event.begin()
  event.message = "Hallo Welt"
  event.counter = 123
  event.commit()
