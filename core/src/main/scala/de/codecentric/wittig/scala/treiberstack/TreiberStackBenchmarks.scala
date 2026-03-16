package de.codecentric.wittig.scala.treiberstack

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
class TreiberStackBenchmarks:

  private var treiberStack: TreiberStack[Int]        = uninitialized
  private var backoffStack: BackoffTreiberStack[Int] = uninitialized
  private var ultraFastStack: UltraFastStack[Int]    = uninitialized

  @Setup
  def setup(): Unit =
    treiberStack = TreiberStack()
    backoffStack = BackoffTreiberStack()
    ultraFastStack = UltraFastStack()

  @Setup(Level.Invocation)
  def resetStacks(): Unit =
    while (treiberStack.pop().isDefined) {}
    while (backoffStack.pop().isDefined) {}
    while (ultraFastStack.pop().isDefined) {}

  @Benchmark
  def pushTreiberStack(): Unit =
    treiberStack.push(ThreadLocalRandom.current().nextInt())

  @Benchmark
  def pushBackoffStack(): Unit =
    backoffStack.push(ThreadLocalRandom.current().nextInt())

  @Benchmark
  def pushUltraFastStack(): Unit =
    ultraFastStack.push(ThreadLocalRandom.current().nextInt())

  @Benchmark
  def popTreiberStack(blackhole: Blackhole): Unit =
    blackhole.consume(treiberStack.pop())

  @Benchmark
  def popBackoffStack(blackhole: Blackhole): Unit =
    blackhole.consume(backoffStack.pop())

  @Benchmark
  def popUltraFastStack(blackhole: Blackhole): Unit =
    blackhole.consume(ultraFastStack.pop())

  @Benchmark
  def mixedTreiberStack(): Unit =
    if ThreadLocalRandom.current().nextBoolean() then
      treiberStack.push(ThreadLocalRandom.current().nextInt())
    else
      treiberStack.pop()

  @Benchmark
  def mixedBackoffStack(): Unit =
    if ThreadLocalRandom.current().nextBoolean() then
      backoffStack.push(ThreadLocalRandom.current().nextInt())
    else
      backoffStack.pop()

  @Benchmark
  def mixedUltraFastStack(): Unit =
    if ThreadLocalRandom.current().nextBoolean() then
      ultraFastStack.push(ThreadLocalRandom.current().nextInt())
    else
      ultraFastStack.pop()

  @Benchmark
  @OperationsPerInvocation(10000)
  def push1000TreiberStack(): Unit =
    var i = 0
    while i < 1000 do
      treiberStack.push(i)
      i += 1

  @Benchmark
  @OperationsPerInvocation(10000)
  def push1000BackoffStack(): Unit =
    var i = 0
    while i < 1000 do
      backoffStack.push(i)
      i += 1

  @Benchmark
  @OperationsPerInvocation(10000)
  def push1000UltraFastStack(): Unit =
    var i = 0
    while i < 1000 do
      ultraFastStack.push(i)
      i += 1
