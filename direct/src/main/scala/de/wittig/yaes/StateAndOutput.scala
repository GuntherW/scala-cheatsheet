package de.wittig.yaes

import in.rcard.yaes.Output
import in.rcard.yaes.State
import in.rcard.yaes.Random
import in.rcard.yaes.Clock

/** State-, Output-, Random- und Clock-Effekte
  *
  * Demonstriert:
  *   - Zustandsverwaltung ohne var / mutables
  *   - Konsolenausgabe als expliziter Effekt (testbar)
  *   - Zufallszahlen deterministisch testbar
  *   - Zeitoperationen als Effekte
  */
object StateAndOutput:

  // ─── Output-Effekt ────────────────────────────────────────────────────────
  // Konsolenausgabe ist ein Effekt – der Typ zeigt, dass die Funktion I/O macht

  def greet(name: String)(using Output): Unit =
    Output.printLn(s"Hallo, $name!")
    Output.printLn(s"Willkommen bei YAES.")

  // ─── State-Effekt ─────────────────────────────────────────────────────────
  // Zustand ohne globale Variable – der Effekt tracked den Zustand im Typ

  // State.run gibt (finalState, result) zurück – der Endzustand und der Rückgabewert
  def runCounter(): (Int, Int) =
    val (finalState, lastValue) = State.run(0) {
      val v1 = State.update[Int](_ + 1)  // +1 → 1
      val v2 = State.update[Int](_ + 1)  // +1 → 2
      val v3 = State.update[Int](_ + 10) // +10 → 12
      State.update[Int](_ + 1) // +1 → 13
    }
    (finalState, lastValue)

  // ─── Random-Effekt ────────────────────────────────────────────────────────

  def rollDice()(using Random): Int = (Random.nextInt % 6).abs + 1

  def simulateGame()(using Random, Output): String =
    val roll = rollDice()
    Output.printLn(s"Würfelwurf: $roll")
    if roll >= 4 then "Gewonnen!" else "Verloren."

  // ─── Clock-Effekt ─────────────────────────────────────────────────────────

  def measureTime[A](label: String)(block: => A)(using Clock, Output): A =
    val start  = Clock.now
    val result = block
    val end    = Clock.now
    Output.printLn(s"$label dauerte: ${java.time.Duration.between(start, end).toMillis}ms")
    result

  // ─── Demo ──────────────────────────────────────────────────────────────────

  @main
  def runStateAndOutput(): Unit =
    println("=== State & Output & Random & Clock Demo ===\n")

    // Output-Effekt
    println("1) Output-Effekt:")
    Output.run(greet("Welt"))
    println()

    // State-Effekt – rein funktional, kein var
    println("2) State-Effekt (Counter):")
    val (finalState, lastVal) = runCounter()
    println(s"   Endzustand: $finalState, letzter Rückgabewert: $lastVal\n")

    // Random-Effekt
    println("3) Random-Effekt:")
    Output.run {
      Random.run {
        val outcome = simulateGame()
        Output.printLn(s"Ergebnis: $outcome")
      }
    }
    println()

    // Clock-Effekt
    println("4) Clock-Effekt:")
    Output.run {
      Clock.run {
        val result = measureTime("Berechnung") {
          Thread.sleep(42)
          42 * 42
        }
        Output.printLn(s"Ergebnis: $result")
      }
    }
