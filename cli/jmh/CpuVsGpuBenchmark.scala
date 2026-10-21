//> using jmh
//> using dep io.computenode::cyfra-foton:0.1.0-RC1

package benchmark

import io.computenode.cyfra.dsl.{*, given}
import io.computenode.cyfra.foton.GFunction
import io.computenode.cyfra.runtime.VkCyfraRuntime
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class CpuVsGpuBenchmark:

  @Param(Array("1000", "100000", "10000000"))
  var size: Int = uninitialized

  var input: Array[Float]                              = uninitialized
  var runtime: VkCyfraRuntime                          = uninitialized
  var gpuFn: GFunction[GStruct.Empty, Float32, Float32] = uninitialized

  // Gleiche Operation auf CPU und GPU: f(x) = 0.5x² + 2x + 1
  private inline def cpuOp(x: Float): Float = x * x * 0.5f + x * 2.0f + 1.0f

  @Setup(Level.Trial)
  def setup(): Unit =
    val rnd = new scala.util.Random(42)
    input = Array.fill(size)(rnd.nextFloat())
    runtime = new VkCyfraRuntime()
    gpuFn = GFunction: (x: Float32) =>
      x * x * 0.5f + x * 2.0f + 1.0f

    // Ergebnisse von CPU und GPU müssen übereinstimmen
    val cpu = cpuMap()
    val gpu = gpuMap()
    val maxDiff = cpu.indices.iterator.map(i => math.abs(cpu(i) - gpu(i))).max
    require(maxDiff < 1e-3f, s"CPU/GPU Ergebnisse weichen ab (maxDiff=$maxDiff)")

  @TearDown(Level.Trial)
  def tearDown(): Unit = runtime.close()

  private def cpuMap(): Array[Float] = input.map(cpuOp)

  private def gpuMap(): Array[Float] =
    given VkCyfraRuntime = runtime
    gpuFn.run(input)

  @Benchmark
  def cpu(): Array[Float] = cpuMap()

  // Enthält Host->Device Upload, Kernel, Device->Host Download
  @Benchmark
  def gpu(): Array[Float] = gpuMap()
