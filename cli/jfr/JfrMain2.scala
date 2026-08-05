package de.codecentric.wittig.scala.jfr

import jdk.jfr.consumer.RecordingFile
import java.nio.file.Path
import java.time.Duration

/** Liest eine .jfr-Datei programmatisch aus und gibt interessante Informationen
  * aus. Dies entspricht dem, was `jfr print` / `jfr view` auf der CLI tut -
  * aber mit voller programmatischer Kontrolle.
  */
@main
def jfrMain2(): Unit =

  val path = Path.of("recording.jfr")
  println(s"Lese: $path\n")

  // RecordingFile gibt einen Iterator über alle Events
  val file = RecordingFile(path)

  var totalEvents  = 0
  var customEvents = 0
  var slowOps      = 0

  while file.hasMoreEvents do
    val event    = file.readEvent()
    val typeName = event.getEventType.getName
    totalEvents += 1

    typeName match
      case "de.codecentric.wittig.scala.jfr.GreetingEvent" =>
        customEvents += 1
        val msg     = event.getString("message")
        val counter = event.getInt("counter")
        println(s"[GreetingEvent] message='$msg', counter=$counter")

      case "de.codecentric.wittig.scala.jfr.SlowOperationEvent" =>
        slowOps += 1
        val name = event.getString("operationName")
        val size = event.getInt("resultSize")
        val dur  = event.getDuration // java.time.Duration
        println(s"[SlowOperationEvent] op='$name', size=$size, duration=${dur.toMillis}ms")

      case "de.codecentric.wittig.scala.jfr.BusinessErrorEvent" =>
        customEvents += 1
        val code = event.getString("errorCode")
        val msg  = event.getString("errorMessage")
        println(s"[BusinessErrorEvent] code='$code', msg='$msg'")

      case "jdk.GarbageCollection" =>
        val gcName = event.getString("name")
        val cause  = event.getString("cause")
        val dur    = event.getDuration
        println(s"[GC] name='$gcName', cause='$cause', duration=${dur.toMillis}ms")

      case "jdk.CPULoad" =>
        val jvm  = event.getFloat("jvmUser")
        val sys  = event.getFloat("machineTotal")
        println(f"[CPULoad] jvmUser=${jvm * 100}%.1f%%, machineTotal=${sys * 100}%.1f%%")

      case _ => // andere Events ignorieren

  file.close()

  println(s"\n--- Zusammenfassung ---")
  println(s"Events gesamt:       $totalEvents")
  println(s"Custom App-Events:   $customEvents")
  println(s"Slow Operations:     $slowOps")
