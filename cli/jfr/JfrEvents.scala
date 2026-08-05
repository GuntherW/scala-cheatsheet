package de.codecentric.wittig.scala.jfr

// JFR Event-Definitionen (Demo-Zwecke)
// Diese Klassen sind keine Geschäftslogik, sondern reine JFR-Instrumentierung.
// Sie dienen ausschließlich dazu, das JFR-Event-System zu demonstrieren.
// In einem realen Projekt würden diese Events die eigene Domäne widerspiegeln
// (z.B. OrderPlacedEvent, PaymentProcessedEvent) und in einem eigenen Modul liegen.

import jdk.jfr.{Category, Description, Event, Label, StackTrace, Threshold}

/** Einfaches Instant-Event ohne Laufzeitmessung. Demonstriert @Label, @Description und @Category. */
@Label("Greeting Event")
@Description("Ein einfaches Begrüßungs-Event zur Demonstration")
@Category(Array("Demo", "Greeting"))
class GreetingEvent extends Event:
  @Label("Message")
  var message: String = ""

  @Label("Counter")
  var counter: Int = 0

/** Duration-Event mit @Threshold: wird nur aufgezeichnet wenn die Dauer den Schwellwert überschreitet.
  * @StackTrace(true) fügt automatisch einen Stack Trace hinzu.
  */
@Label("Slow Operation")
@Description("Misst die Dauer einer Operation - nur aufgezeichnet wenn > 10ms")
@Category(Array("Demo", "Performance"))
@Threshold("10 ms")
@StackTrace(true)
class SlowOperationEvent extends Event:
  @Label("Operation Name")
  var operationName: String = ""

  @Label("Result Size")
  var resultSize: Int = 0

/** Fachliches Fehler-Event - demonstriert, wie Fehler als JFR-Events strukturiert werden. */
@Label("Business Error")
@Description("Fachliche Fehler in der Anwendung")
@Category(Array("Demo", "Errors"))
class BusinessErrorEvent extends Event:
  @Label("Error Code")
  var errorCode: String = ""

  @Label("Error Message")
  var errorMessage: String = ""
