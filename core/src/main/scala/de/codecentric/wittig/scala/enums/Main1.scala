package de.codecentric.wittig.scala.enums

@main
def main1: Unit =

  enum MeinEnum:
    case A(i: Int)
    case B(s: String, d: Double)
    case C

    // Wichtige Funktion: Methoden direkt im Enum definieren
    def description: String = this match
      case A(i) => s"Fall A mit Wert $i"
      case B(s, d) => s"Fall B mit Text '$s' und Zahl $d"
      case C => "Fall C (Singleton)"

  // Wichtige Funktion: Companion Object für Factory-Methoden oder Konstanten
  object MeinEnum:
    def fromInt(i: Int): MeinEnum = if i > 0 then A(i) else C

  val a = MeinEnum.A(3)
  val b = MeinEnum.B("Scala", 3.3)
  val c = MeinEnum.C

  println("--- Standard Ausgabe ---")
  println(a)
  println(b)

  println("\n--- Eigene Methoden im Enum ---")
  println(a.description)
  println(b.description)
  println(c.description)

  println("\n--- Pattern Matching (extern) ---")
  val examples = List(a, b, c)
  examples.foreach {
    case MeinEnum.A(i) => println(s"A gefunden: $i")
    case MeinEnum.B(s, _) => println(s"B gefunden mit String: $s")
    case MeinEnum.C => println("C gefunden")
  }

  println("\n--- Ordinal Values (Index) ---")
  println(s"Ordinal von A: ${a.ordinal}")
  println(s"Ordinal von B: ${b.ordinal}")
  println(s"Ordinal von C: ${c.ordinal}")

  println("\n--- Companion Object Factory ---")
  println(MeinEnum.fromInt(10).description)
  println(MeinEnum.fromInt(-1).description)
