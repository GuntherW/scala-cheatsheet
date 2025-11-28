package de.codecentric.wittig.scala.capturechecking

import language.experimental.captureChecking
import java.io.*
import caps.*

@main
def main(): Unit =

  // auskommentiert, weil scalafmt noch hinfällt.
//  def withFileCap[T](name: String)(f: OutputStream^ => T): T =
//    val out    = new FileOutputStream(name)
//    val result = f(out)
//    out.close()
//    result

  def withFile[T](name: String)(f: OutputStream => T): T =
    val out    = new FileOutputStream(name)
    val result = f(out)
    out.close()
    result

  withFile("nasty.txt")(out => (i: Int) => out.write(i))
//  withFileCap("nasty.txt")(out => (i: Int) => out.write(i))
