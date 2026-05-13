package de.wittig.zioblocks.chunks
import zio.blocks.chunk.*

/** A high-performance, immutable indexed sequence optimized for the patterns common in streaming, parsing, and data processing. Think of it as Vector but faster for the operations that matter most.
  *
  * Full Scala Collection Integration: Implements IndexedSeq for seamless interop.
  */
@main
def main(): Unit = {

  // Create chunks
  val bytes     = Chunk[Byte](1, 2, 3, 4, 5)
  val moreBytes = Chunk.fromArray(Array[Byte](6, 7, 8))

  // Efficient concatenation (O(log n))
  val combined = bytes ++ moreBytes

  // Zero-copy slicing
  val slice = combined.slice(2, 6)

  // Bit operations
  val bits   = bytes.asBitsByte
  val masked = bits & Chunk.fill(bits.length)(true)

  // NonEmptyChunk for type-safe non-emptiness
  val nonEmpty  = NonEmptyChunk(1, 2, 3)
  val head: Int = nonEmpty.head // Always safe, no Option needed
}
