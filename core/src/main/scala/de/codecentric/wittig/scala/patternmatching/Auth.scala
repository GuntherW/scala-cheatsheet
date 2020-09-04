package de.codecentric.wittig.scala.patternmatching

import de.codecentric.wittig.scala.patternmatching.Matching.PremiumUser

trait Auth {
  object as      {
    def unapply(ar: PremiumUser): Option[(String, Int)] = Some(ar.name -> ar.score)
  }
  object asScore {
    def unapply(ar: PremiumUser): Option[Int] = Some(ar.score)
  }
}
