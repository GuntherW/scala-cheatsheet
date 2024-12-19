package de.codecentric.wittig.scala.jfr

import jdk.jfr.{Event, Label}

object JfrMain1 extends App:

  println("Hallo wlet")
  private val event = new GreetingEvent
  event.begin()
  event.message = "Hallo Welt"
  event.counter = 123
  event.commit()

  @Label("mein greeting event")
  private class GreetingEvent extends Event {

    @Label("greeting message")
    var message: String = ""

    @Label("greeting counter")
    var counter: Int = 0
  }
