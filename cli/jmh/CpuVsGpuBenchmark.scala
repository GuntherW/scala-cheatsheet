//> using jmh
//> using dep io.computenode::cyfra-foton:0.1.0-RC1

package benchmark

import io.computenode.cyfra.dsl.{*, given}
import io.computenode.cyfra.dsl.collections.GSeq
import io.computenode.cyfra.foton.GFunction
import io.computenode.cyfra.runtime.VkCyfraRuntime
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized
import scala.util.Random

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class CpuVsGpuBenchmark:

  @Param(Array("100000", "10000000"))
  var size: Int = uninitialized

  var input: Array[Float]                               = uninitialized
  given runtime: VkCyfraRuntime                         = new VkCyfraRuntime()

  // Gleiche Operation auf CPU und GPU: f(x) = 0.5x² + 2x + 1
  val cpuFn: Float => Float                             = x => x * x * 0.5f + x * 2.0f + 1.0f
  val gpuFn: GFunction[GStruct.Empty, Float32, Float32] = GFunction[Float32, Float32](x => x * x * 0.5f + x * 2.0f + 1.0f)

  // Rechenintensiv: iterations Schritte pro Element, damit die Rechenzeit die Transferzeit übersteigt
  val iterations = 200
  val cpuHeavyFn: Float => Float =
    x => (0 until iterations).foldLeft(x)((acc, _) => acc * 0.999f + x * 0.001f)
  val gpuHeavyFn: GFunction[GStruct.Empty, Float32, Float32] =
    GFunction[Float32, Float32](x => GSeq.gen[Int32](0, _ + 1).limit(iterations).fold[Float32](x, (acc, _) => acc * 0.999f + x * 0.001f))

  @Setup(Level.Trial)
  def setup(): Unit =
    val rnd = new Random(42)
    input = Array.fill(size)(rnd.nextFloat())

    // Ergebnisse von CPU und GPU müssen übereinstimmen
    val cpu     = cpuMap()
    val gpu     = gpuMap()
    val maxDiff = cpu.indices.iterator.map(i => math.abs(cpu(i) - gpu(i))).max
    require(maxDiff < 1e-3f, s"CPU/GPU Ergebnisse weichen ab (maxDiff=$maxDiff)")

    val cpuHeavy     = cpuHeavyMap()
    val gpuHeavy     = gpuHeavyMap()
    val maxDiffHeavy = cpuHeavy.indices.iterator.map(i => math.abs(cpuHeavy(i) - gpuHeavy(i))).max
    require(maxDiffHeavy < 1e-3f, s"CPU/GPU Ergebnisse (heavy) weichen ab (maxDiff=$maxDiffHeavy)")

  @TearDown(Level.Trial)
  def tearDown(): Unit = runtime.close()

  private def cpuMap(): Array[Float] = input.map(cpuFn)

  private def gpuMap(): Array[Float] = gpuFn.run(input)

  private def cpuHeavyMap(): Array[Float] = input.map(cpuHeavyFn)

  private def gpuHeavyMap(): Array[Float] = gpuHeavyFn.run(input)

  @Benchmark
  def cpu(): Array[Float] = cpuMap()

  @Benchmark
  def gpu(): Array[Float] = gpuMap()

  @Benchmark
  def cpuHeavy(): Array[Float] = cpuHeavyMap()

  @Benchmark
  def gpuHeavy(): Array[Float] = gpuHeavyMap()
