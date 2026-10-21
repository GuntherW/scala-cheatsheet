//> using jmh
//> using dep it.unimi.dsi:fastutil:8.5.19

package benchmark

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized
import java.util.HashMap as JavaHashMap
import scala.collection.mutable.HashMap as ScalaHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class SearchCacheBenchmark {

  @Param(Array("10000", "1000000"))
  var size: Int = uninitialized

  var javaMap: JavaHashMap[java.lang.Long, String] = uninitialized
  var scalaMap: ScalaHashMap[Long, String]         = uninitialized
  var fastutilMap: Long2ObjectOpenHashMap[String]  = uninitialized
  var testKeys: Array[Long]                        = uninitialized
  var idx: Int                                     = 0

  @Setup(Level.Trial)
  def setup(): Unit = {
    val rnd = new scala.util.Random(42)
    testKeys = Iterator.continually(rnd.nextLong()).distinct.take(size).toArray

    javaMap = new JavaHashMap[java.lang.Long, String](size * 2)
    scalaMap = new ScalaHashMap[Long, String](size * 2, ScalaHashMap.defaultLoadFactor)
    fastutilMap = new Long2ObjectOpenHashMap[String](size)

    testKeys.foreach { key =>
      val value = s"search_result_$key"
      javaMap.put(key, value)
      scalaMap.put(key, value)
      fastutilMap.put(key, value)
    }
  }

  private def nextKey(): Long = {
    idx = (idx + 1) % size
    testKeys(idx)
  }

  @Benchmark
  def lookupJavaHashMap(): String =
    javaMap.get(nextKey())

  @Benchmark
  def lookupScalaHashMap(): String =
    scalaMap.getOrElse(nextKey(), null)

  @Benchmark
  def lookupFastutilMap(): String =
    fastutilMap.get(nextKey())
}
