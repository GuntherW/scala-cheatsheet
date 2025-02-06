package de.wittig.macros.rockthejvm.inlines

object ATransparentInlines extends App:

  // Durch das Schlüsselwort transparent kann der Compiler den Rückgabetyp der Methode noch enger ableiten.
  inline def wrap(i: Int): Option[Int]              = Some(i)
  transparent inline def wrap2(i: Int): Option[Int] = Some(i)

  val a = wrap(42)  // Option[Int]
  val b = wrap2(42) // Some[Int]

  // Warum also nicht jegliche inline Funktionen transparent machen?
  // -> "transparent" auf großen oder rekursiven Types kann den Compiler stark verlangsamen.
