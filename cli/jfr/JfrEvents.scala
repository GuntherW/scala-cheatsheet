package de.codecentric.wittig.scala.jfr

// JFR-Instrumentierung für Performance-Diagnose.
// Zweck: Slow Queries in Korrelation mit JVM-internen Ereignissen (GC, Safepoints)
// sichtbar machen - etwas, das OpenTelemetry nicht leisten kann, weil es keinen
// Zugriff auf JVM-Internals hat.
//
// Das einzige Custom-Event hier ist DbQueryEvent. Alles andere (GC-Pausen,
// Safepoints, Heap-Nutzung) kommt automatisch von der JVM - wir müssen es
// nur mit unserem Event zeitlich in Beziehung setzen.

import jdk.jfr.{Category, Description, Event, Label, StackTrace, Threshold}

/** Misst die Dauer einer Datenbankabfrage.
  *
  * Threshold "50 ms": Im Normalbetrieb sind Queries schnell und erzeugen kein
  * Rauschen. Wird eine Query durch eine GC-Pause verzögert, überschreitet sie
  * den Threshold - und JFR zeichnet sie auf, zusammen mit den GC-Events die
  * zeitgleich stattfanden.
  *
  * Das ist der Kernnutzen: Nicht "welche Queries sind langsam" (das kann ein
  * Query-Logger auch), sondern "*warum* wurden sie langsam - und was hat die
  * JVM in diesem Moment gemacht?"
  */
@Label("DB Query")
@Description("Dauer einer Datenbankabfrage - Korrelation mit GC-Pausen sichtbar machen")
@Category(Array("Performance", "Database"))
@Threshold("10 ms")
@StackTrace(true)
class DbQueryEvent extends Event:

  @Label("SQL")
  var sql: String = ""

  @Label("Table")
  var table: String = ""

  @Label("Rows Returned")
  var rowsReturned: Int = 0
