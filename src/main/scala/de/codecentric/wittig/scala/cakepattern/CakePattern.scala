package de.codecentric.wittig.scala.cakepattern

/**
  * Das Cake Pattern ist eine Art der Dependency Injection mit Scala
  * Die Dependency muß bei Erzeugung des Objectes passieren.
  *
  * http://jonasboner.com/2008/10/06/real-world-scala-dependency-injection-di/
  *
  * Offene Frage für mich: Was ist da jetzt der tolle Vorteil gegenüber dem Weg, daß man die Dependency als Konstruktorparameter übergibt?
  *
  */
class Wesen(name: String) {
  /**
    * Selftype: Zwingt bei Benutzung der Klasse ein Lebewesen reinzumixen (mit with) <br />
    * Hier kann anstatt "this" auch ein Alias stehen, der dann innerhalb der Klasse benutzt werden kann.
    */
  this: Lebewesen =>

  def faehigkeiten = s"Hallo Welt, ich bin $name und ich kann essen: $essen"
}

trait Lebewesen {
  def essen: String
}

trait Mensch extends Lebewesen {
  override def essen = "Loß mer ens vürnehm sin un benotze Metz un Jaffel!"
}

trait Tier extends Lebewesen {
  override def essen = "Schmatzen, reißen, schlingen!"
}

/**
  * Testaufruf
  */
object Main extends App {
  val goldi = new Wesen("Goldi") with Tier
  println(goldi.faehigkeiten)

}
