package bench

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 8, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
class Benchmarks:

  private def runAllocationLoop(objectCount: Int, blackhole: Blackhole): Long =
    val objects = new Array[TinyObject](objectCount)
    var i = 0
    while i < objectCount do
      objects(i) = new TinyObject(i, i ^ 0x5a5a5a5a)
      i += 1

    var sum = 0L
    i = 0
    while i < objectCount do
      val value = objects(i)
      sum += value.a + value.b
      i += 1

    blackhole.consume(objects)
    blackhole.consume(sum)
    sum

  @Benchmark
  @Fork(
    value = 2,
    jvmArgsAppend = Array("-XX:+UnlockExperimentalVMOptions", "-XX:-UseCompactObjectHeaders"),
  )
  def allocateWithoutCompactHeaders(state: AllocationState, blackhole: Blackhole): Long =
    runAllocationLoop(state.objectCount, blackhole)

  @Benchmark
  @Fork(
    value = 2,
    jvmArgsAppend = Array("-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders"),
  )
  def allocateWithCompactHeaders(state: AllocationState, blackhole: Blackhole): Long =
    runAllocationLoop(state.objectCount, blackhole)

@State(Scope.Thread)
class AllocationState:
  @Param(Array("200000", "500000"))
  var objectCount: Int = 0

final class TinyObject(val a: Int, val b: Int)
