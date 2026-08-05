package de.codecentric.wittig.scala.jfr

import jdk.jfr.consumer.{RecordedEvent, RecordingFile}
import java.nio.file.Path
import java.time.Instant

// ---------------------------------------------------------------------------
// Programmatische Korrelationsanalyse: Slow Queries vs. GC-Pausen
//
// Was die JFR-CLI nicht kann: Events aus verschiedenen Typen zeitlich
// in Beziehung setzen und daraus eine Diagnose ableiten.
// Genau das macht diese Datei: Für jede langsame Query wird geprüft,
// ob im selben Zeitfenster eine GC-Pause stattfand - und wenn ja, wie viel
// der Query-Dauer durch GC erklärt werden kann.
// ---------------------------------------------------------------------------

case class SlowQuery(
  sql: String,
  table: String,
  startTime: Instant,
  durationMs: Long,
  gcDuringMs: Long // wie viel davon war GC-Pause
)

case class GcPause(
  startTime: Instant,
  durationMs: Long,
  cause: String
)

@main
def jfrMain2(): Unit =

  val path = Path.of("recording.jfr")
  println(s"Korrelationsanalyse: $path\n")

  val file      = RecordingFile(path)
  val slowQueries = collection.mutable.ListBuffer.empty[SlowQuery]
  val gcPauses    = collection.mutable.ListBuffer.empty[GcPause]

  // Einmal über alle Events iterieren, zwei Listen aufbauen
  while file.hasMoreEvents do
    val event    = file.readEvent()
    val typeName = event.getEventType.getName

    typeName match
      case "de.codecentric.wittig.scala.jfr.DbQueryEvent" =>
        // DbQueryEvent ist nur in der Datei wenn > Threshold (50ms)
        slowQueries += SlowQuery(
          sql        = event.getString("sql"),
          table      = event.getString("table"),
          startTime  = event.getStartTime,
          durationMs = event.getDuration.toMillis,
          gcDuringMs = 0 // wird unten berechnet
        )

      case "jdk.GCPhasePause" =>
        // Stop-the-World-Pausen: in dieser Zeit sind ALLE Threads eingefroren
        gcPauses += GcPause(
          startTime  = event.getStartTime,
          durationMs = event.getDuration.toMillis,
          cause      = event.getString("name")
        )

      case _ =>

  file.close()

  // Korrelation berechnen: Wie viel GC-Zeit fiel in das Zeitfenster jeder Query?
  val correlated = slowQueries.map: q =>
    val queryEnd = q.startTime.plusMillis(q.durationMs)
    val gcOverlap = gcPauses
      .filter: gc =>
        val gcEnd = gc.startTime.plusMillis(gc.durationMs)
        // Zeitfenster überlappen sich?
        gc.startTime.isBefore(queryEnd) && gcEnd.isAfter(q.startTime)
      .map(_.durationMs)
      .sum
    q.copy(gcDuringMs = gcOverlap)

  // Ausgabe
  println(s"Langsame Queries (>10ms Threshold): ${correlated.size}")
  println(s"GC-Pausen gesamt:                       ${gcPauses.size}")
  println()

  if correlated.isEmpty then
    println("Keine langsamen Queries aufgezeichnet - alle unter dem Threshold.")
  else
    println(f"${"Tabelle"}%-15s ${"Query-Zeit"}%10s ${"davon GC"}%10s ${"GC-Anteil"}%10s  SQL")
    println("-" * 90)
    correlated.foreach: q =>
      val gcShare = if q.durationMs > 0 then q.gcDuringMs * 100 / q.durationMs else 0
      val marker  = if gcShare > 50 then " <-- GC-Verdacht!" else ""
      println(f"${q.table}%-15s ${q.durationMs}%8d ms ${q.gcDuringMs}%8d ms ${gcShare}%8d %%  ${q.sql.take(40)}$marker")

  println()
  println("GC-Pausen:")
  gcPauses.foreach: gc =>
    println(f"  ${gc.startTime} ${gc.durationMs}%6d ms  ${gc.cause}")
