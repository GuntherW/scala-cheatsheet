package de.codecentric.wittig.scala.monix
import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import cats.implicits._
import de.codecentric.wittig.scala.Implicits._
import de.codecentric.wittig.scala.Printer._
import monix.execution.Scheduler

import scala.annotation.tailrec

object MonixWithScheduler extends TaskApp {

  private val list = List.tabulate(10)(i => 1000000 + i)

  // Try out different Scheduler
  //  private val schedulerIO             = Scheduler.io(name = "io")
  //  private val schedulerSingleThreaded = Scheduler.singleThread(name = "singleThread")
  //  private val schedulerComputation02  = Scheduler.computation(name = "computation2", parallelism = 2)
  private val schedulerComputation04 = Scheduler.computation(name = "computation4", parallelism = 4)

  private val task1 = parallel(printlnBlue).executeOn(schedulerComputation04)
  private val task2 = parallel(printlnCyan)

  private def programm =
    (task1, task2).parMapN { case (list1, list2) =>
      list1.sum + list2.sum // Addition ohne Bedeutung.
    }

  override def run(args: List[String]): Task[ExitCode] =
    programm.as(ExitCode.Success)

  private def parallel(printlnColored: String => Unit): Task[List[BigInt]] =
    list.zipWithIndex
      .parTraverse { case (i, index) =>
        Task(fibonacci(i)) <*
          Task(printlnColored(s"Lauf: $index,  ThreadId: ${Thread.currentThread().getId}"))
      }

  /** Wird nur als großer CPU-Verbraucher benötigt. Inhalt unwichtig; verbraucht CPU */
  private def fibonacci(i: BigInt): BigInt =
    time {
      @tailrec
      def h(last: BigInt, cur: BigInt, num: BigInt): BigInt =
        if (num == 0) cur
        else h(cur, last + cur, num - 1)

      if (i < 0) -1
      else if (i == 0 || i == 1) 1
      else h(1, 2, i - 2)
    }
}
