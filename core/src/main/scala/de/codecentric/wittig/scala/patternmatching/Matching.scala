package de.codecentric.wittig.scala.patternmatching

/** @author gunther
  */
object Matching extends App with Auth {
  trait User {
    def name: String
    def score: Int
  }
  class FreeUser(val name: String, val score: Int, val upgradeProbability: Double) extends User
  class PremiumUser(val name: String, val score: Int)                              extends User

  object FreeUser {
    def unapply(user: FreeUser): Option[(String, Int, Double)] = Some((user.name, user.score, user.upgradeProbability))
  }

  object PremiumUser {
    def unapply(user: PremiumUser): Option[(String, Int)] = Some((user.name, user.score))
  }

  // unapply kann auch nur einen Boolean zurückgeben. In manchen Situationen möglicherweise schöner.
  object PremiumCandidate {
    def unapply(user: FreeUser): Boolean = user.upgradeProbability > 0.75
  }

  val user: User = new FreeUser("Daniela", 3000, 0.7d)

  def result =
    user match {
      case candidate @ PremiumCandidate() => s"${candidate.name}, Du bist auf einem guten Weg zum Premiumnutzer"
      case FreeUser(name, _, _)           => s"$name, was kann ich für Dich tun?"
      case PremiumUser(name, _)           => s"Herzlich Willkommen, $name"
      case _                              => "no match"
    }

  println(result)

  val premiumUser = new PremiumUser("Daniela", 3000)

  // Infix pattern matching
  premiumUser match {
    case (name @ "Daniela") as score => println(s"Score inline: $score. $name")
    case name as score               => println(s"Score: $score. $name")
    //case as(name, score)             => println(s"Score: $score. $name")
    case _                           => println("no match")
  }
  premiumUser match {
    case asScore(score) => println(s"Score ist $score")
    case _              => println(s"no match")
  }
}
