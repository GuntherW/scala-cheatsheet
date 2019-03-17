package de.codecentric.wittig.scala.monix
import de.codecentric.wittig.scala.Implicits.RichFuture
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

object Evaluation extends App {

  // Strict evaluation
  val a = Task.now { println("effect a"); "immediate" }

  // Lazy evaluation  -- Shortcut zu Task.eval
  val b = Task { println("effect b"); "lazy" }

  // Lazy / memoized evaluation
  val c = Task.evalOnce { println("effect c"); "memoized" } // kann auch mit c.memoize oder c.memoizeOnSuccess erreicht werden

  // Equivalent to a function
  val d = Task.eval { println("effect d"); "always" }

  // Builds a factory of tasks
  val e = Task.defer { Task.now { println("effect e"); "always" } }

  // Guarantees asynchronous execution
  val f = Task.fork(Task.eval { println("effect f"); "hallo" })

  b.runToFuture.await
  c.runToFuture.await
  c.runToFuture.await
  d.runToFuture.await
  e.runToFuture.await
  f.runToFuture.await

  val b2 = b.memoizeOnSuccess
  b2.runToFuture.await
  b2.runToFuture.await // gibt gecachtes Ergebnis zurück. Wird nicht mehr ausgeführt.
}
