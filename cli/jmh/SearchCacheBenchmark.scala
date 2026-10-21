//> using jmh
//> using dep it.unimi.dsi:fastutil:8.5.19

package benchmark

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized
import java.util.HashMap as JavaHashMap
import scala.collection.mutable.HashMap as ScalaMutableHashMap
import scala.collection.immutable.HashMap as ScalaImmutableHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class SearchCacheBenchmark {

  var size: Int = 5000000

  var javaMap: JavaHashMap[java.lang.Long, String]      = uninitialized
  var mutableMap: ScalaMutableHashMap[Long, String]     = uninitialized
  var fastutilMap: Long2ObjectOpenHashMap[String]       = uninitialized
  var immutableMap: ScalaImmutableHashMap[Long, String] = uninitialized
  var bitsetMap: MappedBitset                           = uninitialized
  var testKeys: Array[Long]                             = uninitialized
  var idx: Int                                          = 0

  @Setup(Level.Trial)
  def setup(): Unit = {
    val rnd = new scala.util.Random(42)
    // Bitset braucht einen begrenzten Key-Bereich: Keys liegen in [0, size * 4)
    testKeys = Iterator.continually(rnd.nextInt(size * 4).toLong).distinct.take(size).toArray

    javaMap = new JavaHashMap[java.lang.Long, String](size * 2)
    mutableMap = new ScalaMutableHashMap[Long, String](size * 2, ScalaMutableHashMap.defaultLoadFactor)
    fastutilMap = new Long2ObjectOpenHashMap[String](size)

    testKeys.foreach { key =>
      val value = s"search_result_$key"
      javaMap.put(key, value)
      mutableMap.put(key, value)
      fastutilMap.put(key, value)
    }

    immutableMap = ScalaImmutableHashMap.from(testKeys.map(k => k -> s"search_result_$k"))
    val sorted = testKeys.sorted
    bitsetMap = new MappedBitset(sorted.map(_.toInt), sorted.map(k => s"search_result_$k"))
  }

  private def nextKey(): Long = {
    idx = (idx + 1) % size
    testKeys(idx)
  }

  @Benchmark
  def lookupJavaHashMap(): String =
    javaMap.get(nextKey())

  @Benchmark
  def lookupScalaMutableHashMap(): String =
    mutableMap.getOrElse(nextKey(), null)

  @Benchmark
  def lookupFastutilMap(): String =
    fastutilMap.get(nextKey())

  @Benchmark
  def lookupScalaImmutableHashMap(): String =
    immutableMap.getOrElse(nextKey(), null)

  @Benchmark
  def lookupMappedBitset(): String =
    bitsetMap.get(nextKey())
}

/** Bitset markiert vorhandene Keys, Werte liegen dicht im Array. Lookup: Bit prüfen, Rang per Popcount bestimmen. */
final class MappedBitset(sortedKeys: Array[Int], sortedValues: Array[String]) {
  private val words: Array[Long] = new Array[Long]((sortedKeys.last >>> 6) + 1)
  private val prefix: Array[Int] = new Array[Int](words.length)
  private val values             = sortedValues

  sortedKeys.foreach(k => words(k >>> 6) |= 1L << (k & 63))
  words.indices.drop(1).foreach(w => prefix(w) = prefix(w - 1) + java.lang.Long.bitCount(words(w - 1)))

  def get(key: Long): String = {
    val w = key >>> 6
    if (w >= words.length) null
    else {
      val bit  = 1L << (key & 63).toInt
      val word = words(w.toInt)
      if ((word & bit) == 0L) null
      else values(prefix(w.toInt) + java.lang.Long.bitCount(word & (bit - 1)))
    }
  }
}
