package de.codecentric.wittig.scala.patternmatching

/** @author
  *   gunther
  */
object Matching extends App {
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

  object as {
    infix def unapply(ar: PremiumUser): Option[(String, Int)] = Some((ar.name, ar.score))
  }

  object asScore {
    def unapply(ar: PremiumUser): Option[Int] = Some(ar.score)
  }

  // Infix pattern matching
  val p1 = premiumUser match {
    case (name @ "Daniela") as score => s"Score inline: $score. $name"
    case name as score               => s"Score: $score. $name"
//    case as(name, score)             => s"Score: $score. $name" the same
    case _                           => "no match"
  }
  println(p1)

  val p2 = premiumUser match {
    case asScore(score) => s"Score ist $score"
    case _              => s"no match"
  }
  println(p2)

  val list1           = List(1, 2, 3, 4)
  val listDescription = list1 match {
    case List(1, 2, _)  => "list with 1,2 and a third element"
    case List(1, 2, _*) => "list with 1,2 and something else"
    case _              => "empty listq"
  }
  println(listDescription)
}
