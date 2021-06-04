package de.codecentric.wittig.scala.model

/** Funktionalitäten und Verhalten der Klasse können bequem über Traits eingebunden werden Im Test kann der Trait mit einem Mock überschrieben werden: z.B. val p = Person("Gunther", 37) with AuthMock
  */
case class Person(name: String, alter: Int) extends Auth {
  // Alle Vorbedingungen mit "require" absichern
  require(alter > 18)
}
