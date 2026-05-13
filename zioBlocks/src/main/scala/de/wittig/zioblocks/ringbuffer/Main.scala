package de.wittig.zioblocks.ringbuffer
import zio.blocks.ringbuffer._

/** High-performance, bounded ring buffers for inter-thread communication. Four lock-free variants cover every producer/consumer pattern (SPSC, MPSC, SPMC, MPMC).
  */
@main
def main(): Unit =

  // SPSC: fastest, for dedicated producer-consumer pairs
  val spsc = SpscRingBuffer[String](1024)
  spsc.offer("hello") // true
  spsc.take()         // "hello"

  // MPMC: general-purpose, any number of threads
  val mpmc = MpmcRingBuffer[String](1024)
  mpmc.offer("hello") // false if full
  mpmc.take()
