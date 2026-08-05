# Java Flight Recorder (JFR)

JFR ist ein in die JVM eingebautes Low-Overhead-Profiling- und Diagnose-Framework.
Es zeichnet Events auf - sowohl JVM-interne (GC, Threads, CPU, Klassen) als auch eigene
Application-Events - und speichert sie in einer binären `.jfr`-Datei.

## Konzepte

### Events

Alles in JFR dreht sich um **Events**. Ein Event hat:
- einen **Typ** (Name, Label, Kategorie)
- einen **Zeitstempel** (wann es begann, wie lange es dauerte)
- **Felder** (beliebige Nutzdaten)
- optional einen **Stack Trace**

Eigene Events werden durch Ableiten von `jdk.jfr.Event` erstellt:

```scala
@Label("Slow Operation")
@Category(Array("Demo", "Performance"))
@Threshold("10 ms")   // nur aufzeichnen wenn Dauer > 10ms
@StackTrace(true)     // Stack Trace mitaufzeichnen
class SlowOperationEvent extends Event:
  var operationName: String = ""
  var resultSize: Int       = 0
```

Verwendung im Code:

```scala
val event = new SlowOperationEvent
event.begin()                       // Startzeit festhalten
event.operationName = "db-query"
event.resultSize   = 1500
Thread.sleep(50)
event.commit()                      // aufzeichnen (falls Threshold erfüllt)
```

### Threshold

Mit `@Threshold("10 ms")` wird das Event **nur dann** in die Datei geschrieben,
wenn die Dauer zwischen `begin()` und `commit()` den Schwellwert überschreitet.
Das hält die Datei klein und filtert "unwichtige" schnelle Aufrufe heraus.

### Settings / Profile

JFR kennt zwei vordefinierte Konfigurationsprofile:
- `default` - geringer Overhead, für Produktion geeignet
- `profile`  - mehr Details (Stack Traces, mehr Events), für Entwicklung/Analyse

### Aufzeichnung starten

**Via JVM-Flag beim Start:**

```shell
scala-cli run JfrMain1.scala \
  --java-opt "-XX:StartFlightRecording=name=Demo,settings=profile,dumponexit=true,filename=recording.jfr"
```

**Via `jcmd` an laufende JVM:**

```shell
# PID ermitteln
jcmd                                              # listet alle JVMs

# Aufzeichnung starten
jcmd <PID> JFR.start name=MyRecording settings=profile filename=recording.jfr

# Status abfragen
jcmd <PID> JFR.check

# Dump der aktuellen Daten (ohne Stop)
jcmd <PID> JFR.dump name=MyRecording filename=snapshot.jfr

# Aufzeichnung stoppen
jcmd <PID> JFR.stop name=MyRecording filename=recording.jfr
```

---

## JFR CLI-Werkzeuge

### `jfr summary` - Überblick

Zeigt alle Event-Typen und ihre Anzahl:

```shell
jfr summary recording.jfr
```

```
Event Type                                        Count  Size (bytes)
=======================================================================
jdk.GarbageCollection                                 1            23
de.codecentric.wittig.scala.jfr.SlowOperationEvent    1            30
de.codecentric.wittig.scala.jfr.GreetingEvent         2            40
...
```

### `jfr view` - Tabellen-Ansicht

Vorgefertigte Views für häufige Analysen:

```shell
# Alle Events anzeigen
jfr view all-events recording.jfr

# JVM-Informationen
jfr view jvm-information recording.jfr

# Garbage Collection
jfr view gc recording.jfr

# Heiße Methoden (CPU-Profiling)
jfr view hot-methods recording.jfr

# Speicher-Allokationen nach Klasse
jfr view allocation-by-class recording.jfr

# Exceptions
jfr view exception-by-type recording.jfr

# Threads
jfr view thread-cpu-load recording.jfr
jfr view thread-start recording.jfr

# CPU-Auslastung
jfr view cpu-load recording.jfr

# Systeminfo
jfr view system-information recording.jfr
jfr view environment-variables recording.jfr

# Eigenes Event direkt als View
jfr view de.codecentric.wittig.scala.jfr.GreetingEvent recording.jfr

# Alle verfügbaren Views anzeigen
jfr view all-views recording.jfr

# Mit Details (zeigt die zugrundeliegende Query)
jfr view --verbose hot-methods recording.jfr
```

### `jfr print` - Rohausgabe

Gibt Events im Text-, XML- oder JSON-Format aus - ideal für Scripting:

```shell
# Alle Events als Text
jfr print recording.jfr

# Nur bestimmte Events
jfr print --events GarbageCollection recording.jfr
jfr print --events "de.codecentric.*" recording.jfr

# Mehrere Events
jfr print --events CPULoad,GarbageCollection recording.jfr

# Ganze Kategorien
jfr print --categories "GC,JVM" recording.jfr

# Als JSON (für weitere Verarbeitung)
jfr print --json --events CPULoad recording.jfr

# Als XML
jfr print --xml --events "jdk.*" recording.jfr

# Mit vollständigen Stack Traces (Standard: 5 Frames)
jfr print --stack-depth 20 --events OldObjectSample recording.jfr
```

### `jfr metadata` - Event-Schema

Zeigt die Struktur der Event-Typen (Felder, Typen, Annotations):

```shell
# Alle Event-Typen
jfr metadata recording.jfr

# Nur eigene Events
jfr metadata --events "de.codecentric.*" recording.jfr

# Nur GC-Events
jfr metadata --categories GC recording.jfr
```

### `jfr scrub` - Datei bereinigen

Erstellt eine gefilterte Kopie - z.B. um sensitive Daten zu entfernen:

```shell
# Nur eigene App-Events behalten
jfr scrub --include-events "de.codecentric.*" recording.jfr app-only.jfr

# Bestimmte Events entfernen
jfr scrub --exclude-events jdk.InitialEnvironmentVariable recording.jfr clean.jfr
```

---

## Programmatisches Lesen

`JfrMain2.scala` zeigt, wie man `.jfr`-Dateien mit der `RecordingFile`-API in Scala liest:

```shell
scala-cli run JfrMain2.scala
```

Kern-API:

```scala
import jdk.jfr.consumer.RecordingFile
import java.nio.file.Path

val file = RecordingFile(Path.of("recording.jfr"))
while file.hasMoreEvents do
  val event    = file.readEvent()
  val typeName = event.getEventType.getName
  val duration = event.getDuration      // java.time.Duration
  val msg      = event.getString("message")
  val count    = event.getInt("counter")
file.close()
```

---

## Dateien in diesem Projekt

| Datei            | Beschreibung |
|------------------|-------------|
| `project.scala`  | Scala CLI Projektkonfiguration (Scala-Version, JVM, Default-Main) |
| `JfrEvents.scala` | Custom JFR-Event-Klassen (Instrumentierung, keine Geschäftslogik) |
| `JfrMain1.scala` | Demonstriert das Aufzeichnen von Custom-Events |
| `JfrMain2.scala` | Liest `recording.jfr` programmatisch mit der `RecordingFile`-API aus |
| `recording.jfr`  | Aufgezeichnete JFR-Datei vom letzten Run |

## Schnellstart

```shell
cd cli/jfr

# 1. Aufzeichnung erstellen (Default-Main: jfrMain1)
scala-cli run . \
  --java-opt "-XX:StartFlightRecording=name=Demo,settings=profile,dumponexit=true,filename=recording.jfr"

# Explizit eine bestimmte Main-Methode wählen:
scala-cli run . --main-class de.codecentric.wittig.scala.jfr.jfrMain1 \
  --java-opt "-XX:StartFlightRecording=name=Demo,settings=profile,dumponexit=true,filename=recording.jfr"

# 2. Überblick
jfr summary recording.jfr

# 3. Eigene Events anschauen
jfr view de.codecentric.wittig.scala.jfr.GreetingEvent recording.jfr
jfr print --events "de.codecentric.*" recording.jfr

# 4. JVM-Analyse
jfr view jvm-information recording.jfr
jfr view gc recording.jfr
jfr view hot-methods recording.jfr

# 5. Metadaten der eigenen Events
jfr metadata --events "de.codecentric.*" recording.jfr

# 6. Programmatisch auswerten
scala-cli run . --main-class de.codecentric.wittig.scala.jfr.jfrMain2
```
