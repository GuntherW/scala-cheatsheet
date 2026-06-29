package de.codecentric.wittig.scala.capturechecking.rtjvm

import language.experimental.captureChecking
import caps.*

/**
 * `->` vs `=>`  in Scala 3 Capture Checking
 *
 * `A => B`  – normale Funktion; darf beliebige Capabilities einfangen (impure)
 * `A -> B`  – pure Funktion;   darf keine Capabilities einfangen
 *
 * Eine Capability ist ein Wert vom Typ `T^` (tracked/captured).
 * `->` ist syntaktischer Zucker für `(x: A) => B` with `caps.cap ⊢ {}`
 */
class Logger:
  def log(msg: String): Unit = println(s"[LOG] $msg")

@main
def main(): Unit =

  val logger: Logger^ = Logger()

  // `=>` – impure: darf `logger` (eine Capability) einfangen
  val impure: String => Unit = msg => logger.log(msg)
  impure("Hello via impure =>")

  // `->` – pure: darf *keine* Capabilities einfangen
  val pure: String -> String = msg => msg.toUpperCase
  println(pure("Hello via pure ->"))

  // Das folgende würde NICHT kompilieren:
  // val illegal: String -> Unit = msg => logger.log(msg)
  //   => Fehler: (logger: Logger^) cannot be referenced here
