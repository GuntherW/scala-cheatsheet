package de.codecentric.wittig.scala.jfr

import scala.util.Random
import scala.collection.mutable

// ---------------------------------------------------------------------------
// Das Problem, das dieses Beispiel zeigt:
//
// Eine Anwendung führt regelmäßig DB-Abfragen durch. Die meisten sind schnell
// (~10ms). Gelegentlich aber werden einzelne Abfragen unerwartet langsam (>50ms)
// - ohne dass sich an der Query oder den Daten etwas geändert hat.
//
// Mit einem normalen Query-Logger sieht man nur: "Query X dauerte 120ms."
// Mit JFR sieht man: "Query X dauerte 120ms - und in diesem Zeitfenster fand
// eine GC-Pause von 80ms statt, die den Thread angehalten hat."
//
// Das Custom-Event DbQueryEvent hat @Threshold("50 ms"): Es taucht in der
// Aufzeichnung nur dann auf, wenn eine Query langsam war. Daneben zeichnet die
// JVM automatisch jdk.GCPhasePause, jdk.GarbageCollection und jdk.SafepointBegin
// auf. Die zeitliche Überlappung dieser Events erklärt das Problem.
// ---------------------------------------------------------------------------

// Simulierter Result-Row - repräsentiert geladene Daten
case class Row(id: Int, payload: Array[Byte])

// ---------------------------------------------------------------------------
// Simulierter DB-Zugriff mit JFR-Instrumentierung
// ---------------------------------------------------------------------------

object FakeDatabase:

  // Simuliert eine Query. Die eigentliche "Arbeit" besteht aus:
  // 1. Basis-Latenz (10-20ms) - normale Query-Zeit
  // 2. Allokation von Ergebnis-Rows - erzeugt GC-Druck
  // 3. Die GC-Pause selbst verzögert dann Thread.sleep() zusätzlich,
  //    weil der Thread während einer Stop-the-World-Pause angehalten wird.
  def query(sql: String, table: String, rowCount: Int): List[Row] =
    val event = new DbQueryEvent
    event.begin()
    event.sql   = sql
    event.table = table

    // Basis-Latenz simulieren
    Thread.sleep(Random.between(10, 20))

    // Ergebnis-Rows allokieren - jede Row trägt 10KB Payload.
    // Das passiert *während* das Event läuft (zwischen begin() und commit()).
    // Bei hohem rowCount akkumuliert das und kann mitten in der Query einen GC auslösen.
    val rows = (1 to rowCount).map(i => Row(i, Array.fill(10_000)(i.toByte))).toList

    event.rowsReturned = rows.size
    event.commit() // wird nur aufgezeichnet wenn > 50ms (Threshold)
    rows

// ---------------------------------------------------------------------------
// Anwendungslogik - führt periodisch Queries durch
// ---------------------------------------------------------------------------

object ReportingService:

  def runReports(): Unit =
    val queries = List(
      ("SELECT * FROM orders WHERE status = 'PENDING'",       "orders",    200),
      ("SELECT * FROM products WHERE category = 'ELECTRONICS'", "products", 500),
      ("SELECT * FROM customers WHERE region = 'DE'",          "customers", 300),
      ("SELECT * FROM invoices WHERE due_date < NOW()",         "invoices",  150),
    )

    println("Starte Report-Durchläufe (je 3 Runden)...\n")

    for round <- 1 to 3 do
      println(s"--- Runde $round ---")
      for (sql, table, rows) <- queries do
        val result = FakeDatabase.query(sql, table, rows)
        println(s"  $table: ${result.size} Rows geladen")

      // Zwischen den Runden: gehaltene Referenzen simulieren (z.B. ein wachsender Cache).
      // Das erhöht den Heap-Druck für die nächste Runde - die Queries der nächsten
      // Runde laufen dann unter erschwertem GC-Bedingungen.
      println("  [Heap-Druck aufbauen...]")
      val pressure = (1 to 100).map(_ => Array.fill(50_000)(0.toByte)).toList
      println(s"  [${pressure.size * 50_000 / 1024} KB allokiert, warte auf GC...]")
      Thread.sleep(50)
      println()

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

@main
def jfrMain1(): Unit =
  ReportingService.runReports()

  println("Fertig. Auswerten mit:")
  println()
  println("# Welche Queries waren langsam? (nur Events über dem Threshold)")
  println("  jfr print --events de.codecentric.wittig.scala.jfr.DbQueryEvent recording.jfr")
  println()
  println("# GC-Aktivität im selben Zeitfenster:")
  println("  jfr view gc recording.jfr")
  println("  jfr print --events GarbageCollection,GCPhasePause recording.jfr")
  println()
  println("# Heap-Entwicklung und Allokationen:")
  println("  jfr view allocation-by-class recording.jfr")
  println("  jfr view object-statistics recording.jfr")
  println()
  println("# Alles zusammen im Zeitstrahl:")
  println("  jfr print --events 'de.codecentric.*,jdk.GCPhasePause,jdk.GarbageCollection' recording.jfr")
