package de.codecentric.wittig.scala.treiberstack

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

/** https://grokipedia.com/page/treiber_stack
  */
@main
def main(): Unit =

  // Wir erstellen einen Thread-Pool für echte Parallelität
  val executor           = Executors.newFixedThreadPool(8)
  given ExecutionContext = ExecutionContext.fromExecutor(executor)

  val stack       = TreiberStack[Int]()
  val iterations  = 1000
  val range       = 1 to iterations
  val expectedSum = range.sum

  println(s"Starte Test: $iterations parallele Operationen...")

  // 1. Viele Futures pushen gleichzeitig
  val pushTasks = Future.sequence(range.map { i =>
    Future(stack.push(i))
  })

  // 2. Parallel dazu versuchen wir ständig zu poppen
  val poppedValues = AtomicReference[List[Int]](Nil)
  val popTasks     = Future.sequence((1 to iterations).map { _ =>
    Future {
      // Wir versuchen mehrmals zu poppen, falls der Stack kurzzeitig leer ist
      var result: Option[Int] = None
      while (result.isEmpty) result = stack.pop()
      val v                   = result.get

      // Sicherer Update der Ergebnisliste
      @tailrec
      def updateList(): Unit =
        val old = poppedValues.get()
        if !poppedValues.compareAndSet(old, v :: old) then updateList()
      updateList()
    }
  })

  // Warten auf Abschluss
  Await.result(Future.sequence(List(pushTasks, popTasks)), 10.seconds)

  val results   = poppedValues.get()
  val remaining = stack.popAll()
  val totalSum  = results.sum + remaining.sum

  println(s"Test abgeschlossen!")
  println(s"Gepusht: Summe $expectedSum")
  println(s"Gepoppt: ${results.size} Elemente, Summe ${results.sum}")
  println(s"Im Stack verblieben: ${remaining.size} Elemente")
  println(s"Gesamtsumme: $totalSum")

  if totalSum == expectedSum then
    println("✅ ERFOLG: Keine Daten verloren gegangen!")
  else
    println("❌ FEHLER: Datenkorruption festgestellt!")

  executor.shutdown()
