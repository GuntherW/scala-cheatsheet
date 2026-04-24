package de.codecentric.wittig.scala.capturechecking.beispiel1

import scala.language.experimental.captureChecking
import scala.language.experimental.separationChecking

class Ref(init: Int) extends caps.Mutable:

  private var current = init
  def get: Int = current

  /**
   * update def indicates that the method mutates the class’s state (or external resources, whatever), and these methods cannot be called in a read-only context.
   */
  update def set(x: Int): Unit = {
    current = x
  }