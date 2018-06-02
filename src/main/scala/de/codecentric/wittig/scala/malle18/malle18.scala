package de.codecentric.wittig.scala

package object malle18 {

  def time[R](msg: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println(s"Elapsed time for $msg: " + (t1 - t0) / 1000000 + "ms")
    result
  }
}
