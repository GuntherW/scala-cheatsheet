package de.codecentric.wittig.scala.jfr

@main
def jfrMain1(): Unit =

  println("=== JFR Demo ===")

  // 1. Einfaches Event
  val greet = new GreetingEvent
  greet.begin()
  greet.message = "Hallo Welt"
  greet.counter = 42
  greet.commit()
  println("Greeting Event committed")

  // 2. Schnelle Operation - wird wegen Threshold NICHT aufgezeichnet
  val fast = new SlowOperationEvent
  fast.begin()
  fast.operationName = "fast-op"
  fast.resultSize = 10
  Thread.sleep(5) // 5ms < 10ms Threshold
  fast.commit()
  println("Fast operation done (below threshold, not recorded)")

  // 3. Langsame Operation - wird aufgezeichnet
  val slow = new SlowOperationEvent
  slow.begin()
  slow.operationName = "slow-database-query"
  slow.resultSize = 1500
  Thread.sleep(50) // 50ms > 10ms Threshold
  slow.commit()
  println("Slow operation done (above threshold, recorded!)")

  // 4. Error Event
  val err = new BusinessErrorEvent
  err.begin()
  err.errorCode = "INVALID_INPUT"
  err.errorMessage = "Pflichtfeld 'email' fehlt"
  err.commit()
  println("Business error recorded")

  // 5. should-commit check: Event nur aufzeichnen wenn aktiviert
  val conditional = new GreetingEvent
  if conditional.isEnabled then
    conditional.begin()
    conditional.message = "Conditional commit"
    conditional.counter = 99
    conditional.commit()
    println("Conditional event committed")

  println("=== Done ===")
