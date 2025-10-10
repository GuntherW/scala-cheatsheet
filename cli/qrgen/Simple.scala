//> using dep eu.joaocosta::qrgen::0.1.0

import eu.joaocosta.qrgen.*

@main
def simple =

  val qrCode = QrCode.encodeText("https://www.scala-lang.org/", Ecc.LOW)

  qrCode.foreach { line =>
    println(line.map(x => if (x) "█" * 2 else " " * 2).mkString) // /u2588 (IntelliJ: Strg+Shift+U 2588)
  }
