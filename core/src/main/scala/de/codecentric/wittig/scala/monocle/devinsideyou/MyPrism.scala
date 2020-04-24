package de.codecentric.wittig.scala.monocle.devinsideyou

import monocle.Prism
import monocle.macros.{GenIso, GenPrism}

object MyPrism {

  sealed abstract class Ampel extends Product with Serializable
  object Ampel {
    case class Rot(wert: Double) extends Ampel {
      override def toString: String = Console.RED + "Rot: " + wert + Console.RESET
    }
    case class Gelb(wert: Double) extends Ampel {
      override def toString: String = Console.YELLOW + "Gelb: " + wert + Console.RESET
    }
    case class Gruen(wert: Double) extends Ampel {
      override def toString: String = Console.GREEN + "Grün: " + wert + Console.RESET
    }
  }

  val prism = Prism[Ampel, Ampel.Gruen] {
    case green @ Ampel.Gruen(_) => Some(green)
    case _                      => None
  }(identity)

  // Gleiches Prism wie "prism"
  val prism2 = Prism.partial[Ampel, Ampel.Gruen] {
    case green @ Ampel.Gruen(_) => green
  }(identity)

  // Gleiches Prism wie "prism"
  val prism3 = GenPrism[Ampel, Ampel.Gruen]

  val typicalPrism: Prism[Ampel, Double] =
    Prism.partial[Ampel, Double] {
      case Ampel.Gruen(d) => d
    }(Ampel.Gruen)
  val typicalPrism2: Prism[Ampel, Double] =
    GenPrism[Ampel, Ampel.Gruen]
      .composeIso(GenIso[Ampel.Gruen, Double])

  def run(): Unit = {
    println("-" * 40 + "Prisms" + "-" * 40)
    val rot   = Ampel.Rot(0.6)
    val gelb  = Ampel.Gelb(0.6)
    val gruen = Ampel.Gruen(0.6)
    println(rot)
    println(gelb)
    println(gruen)
    println("-" * 100)
    println(typicalPrism.modify(_ + 1)(rot))
    println(typicalPrism.modify(_ + 1)(gelb))
    println(typicalPrism.modify(_ + 1)(gruen))
    println("-" * 100)
    Seq(prism, prism2, prism3).foreach { prism =>
      println(prism.getOption(rot))
      println(prism.getOption(gelb))
      println(prism.getOption(gruen))
    }
  }
}
