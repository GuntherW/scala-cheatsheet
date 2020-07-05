package de.codecentric.wittig.scala.monix
import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import cats.implicits._
import de.codecentric.wittig.scala.Implicits._
import de.codecentric.wittig.scala.Printer._
import monix.execution.Scheduler
object MonixWithScheduler extends TaskApp {

  private val list        = List.tabulate(10)(i => 1000000 + i)
  private val schedulerIO = Scheduler.io(name = "schedulerIO")
  private val scheduler02 = Scheduler.computation(name = "scheduler02", parallelism = 2)

  override def run(args: List[String]): Task[ExitCode] =
    programm.as(ExitCode.Success)

  def programm = (task1, task2).parMapN { case (list1, list2) => () }
  val task1    = parallel(printlnBlue).executeOn(scheduler02)
  val task2    = parallel(printlnCyan)

  def parallel(printlnColored: String => Unit): Task[List[BigInt]] =
    list.zipWithIndex.parTraverse {
      case (i, index) =>
        Task(fibonacci(i)).map { p =>
          printlnColored(s"Lauf: $index,  ThreadId: ${Thread.currentThread().getId}")
          p
        }
    }

  /** Wird nur als großer CPU-Verbraucher benötigt. Inhalt unwichtig; verbraucht CPU */
  private def fibonacci(i: BigInt): BigInt = time {
    def h(last: BigInt, cur: BigInt, num: BigInt): BigInt = {
      if (num == 0) cur
      else h(cur, last + cur, num - 1)
    }

    if (i < 0) -1
    else if (i == 0 || i == 1) 1
    else h(1, 2, i - 2)
  }
}
