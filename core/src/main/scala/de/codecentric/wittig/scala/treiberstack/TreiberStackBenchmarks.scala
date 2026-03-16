package de.codecentric.wittig.scala.treiberstack

import org.openjdk.jmh.annotations.*
import java.util.concurrent.ThreadLocalRandom

import scala.compiletime.uninitialized
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
class TreiberStackBenchmarks:

  @Param(Array("10000"))
  var iterations: Int = 10000

  private var treiber: TreiberStack[Int]        = uninitialized
  private var backoff: BackoffTreiberStack[Int] = uninitialized
  private var ultra: UltraFastTreiberStack[Int] = uninitialized
  private var ec: ExecutionContext              = uninitialized

  @Setup
  def setup(): Unit =
    ec = ExecutionContext.fromExecutorService(java.util.concurrent.Executors.newFixedThreadPool(8))
    treiber = TreiberStack()
    backoff = BackoffTreiberStack()
    ultra = UltraFastTreiberStack()

  @TearDown
  def shutdown(): Unit =
    ec.asInstanceOf[java.util.concurrent.ExecutorService].shutdown()

  @TearDown(Level.Iteration)
  def reset(): Unit =
    while (treiber.pop().isDefined) {}
    while (backoff.pop().isDefined) {}
    while (ultra.pop().isDefined) {}

  @Benchmark
  def treiberStack(): Unit =
    given ExecutionContext = ec
    runBenchmark(treiber)

  @Benchmark
  def backoffStack(): Unit =
    given ExecutionContext = ec
    runBenchmark(backoff)

  @Benchmark
  def ultraFastStack(): Unit =
    given ExecutionContext = ec
    runBenchmark(ultra)

  private def runBenchmark(stack: Stack[Int])(using ec: ExecutionContext): Unit =
    val range = 0 until iterations

    val pushTasks = Future.sequence(range.map(_ => Future(stack.push(ThreadLocalRandom.current().nextInt()))))
    val popTasks  = Future.sequence(range.map(_ => Future(stack.pop())))

    Await.result(pushTasks, 10.seconds)
    Await.result(popTasks, 10.seconds)
