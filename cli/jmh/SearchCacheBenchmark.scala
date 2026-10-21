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

    immutableMap = ScalaImmutableHashMap.from(mutableMap)
    bitsetMap = new MappedBitset(immutableMap)
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

/** Statische Map von Long-Keys (aus einem begrenzten Bereich 0..maxKey) auf Werte, ohne Hashing und ohne Zeiger-Ketten.
  *
  * Idee: Statt jeden Key zu hashen, steht ein Bit pro möglichem Key in einem Bitset: Bit gesetzt = Key vorhanden. Die Werte liegen dicht (ohne Lücken) in einem Array, sortiert nach Key. Der Index
  * eines Werts ist die Anzahl der gesetzten Bits vor seinem Key (der "Rang"), also per Popcount berechenbar.
  *
  * Beispiel: Keys {1, 5, 6} -> Bits 0110 0010 (Bit 1, 5, 6 gesetzt). Key 6 hat 2 gesetzte Bits davor -> values(2).
  *
  * Warum schnell: Bitset und Werte liegen in flachen Arrays. Ein Lookup braucht nur wenige, vorhersehbare Speicherzugriffe (statt mehrerer Zeiger-Sprünge wie in Hash-Maps oder Bäumen) und das kleine
  * Bitset passt oft in den CPU-Cache.
  *
  * Voraussetzungen: Keys müssen nicht-negativ sein und in einem begrenzten Bereich liegen (Speicher ~ größter Key / 8 Bytes). Die Sortierung nach Key passiert im Konstruktor, die Map ist danach
  * unveränderlich.
  */
final class MappedBitset(entries: Map[Long, String]) {
  // Nach Key sortieren: Die Reihenfolge der Werte im Array muss der Reihenfolge der Bits entsprechen (Rang = Index).
  private val sortedEntries: Array[(Long, String)] = entries.toArray.sortBy(_._1)
  private val sortedKeys: Array[Long]              = sortedEntries.map(_._1)

  // Bitset: Wort w deckt die Keys w*64 .. w*64+63 ab. Länge reicht bis zum größten Key (sortiert -> letzter).
  private val words: Array[Long] = new Array[Long](((sortedKeys.last >>> 6) + 1).toInt)

  // prefix(w) = Anzahl gesetzter Bits in allen Wörtern VOR Wort w. Spart beim Lookup das Zählen über alle vorherigen Wörter (O(1) statt O(Wörter)).
  private val prefix: Array[Int] = new Array[Int](words.length)

  // Dichtes Werte-Array, gleiche Reihenfolge wie die sortierten Keys.
  private val values: Array[String] = sortedEntries.map(_._2)

  // Bit für jeden Key setzen: k >>> 6 = k / 64 (Wort), k & 63 = k % 64 (Bit im Wort).
  sortedKeys.foreach(k => words((k >>> 6).toInt) |= 1L << (k & 63).toInt)

  // Laufende Summe der Popcounts: Wort w startet bei prefix(w-1) + Bits von Wort w-1. Wort 0 startet bei 0.
  words.indices.drop(1).foreach(w => prefix(w) = prefix(w - 1) + java.lang.Long.bitCount(words(w - 1)))

  def get(key: Long): String = {
    val w = key >>> 6 // Welches Wort? (Long, damit negative/riesige Keys sicher als "außerhalb" erkannt werden)
    if (w >= words.length) null
    else {
      val bit  = 1L << (key & 63).toInt // Maske für das Bit des Keys innerhalb des Worts
      val word = words(w.toInt)
      if ((word & bit) == 0L) null // Bit nicht gesetzt: Key existiert nicht
      // Index = Bits in früheren Wörtern + Bits im selben Wort unterhalb des Keys (bit - 1 = Maske aller niedrigeren Bits)
      else values(prefix(w.toInt) + java.lang.Long.bitCount(word & (bit - 1)))
    }
  }
}
