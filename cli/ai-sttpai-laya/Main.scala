package laya

import sttp.ai.jev.{Choice, JevSyncClient, Noul, Score}

import scala.util.Using

given Using.Releasable[JevSyncClient] = _.close()

/** Einstiegspunkt: Stellt drei typisierte Fragen (Choice/Score/Noul) zu einem Beispiel-Ticket an einen lokal laufenden `laya-serve`.
  *
  * Voraussetzung: `laya-serve` läuft bereits lokal (siehe README.md).
  *
  * Aufruf:
  * {{{
  *   scala-cli run .
  * }}}
  */
@main def main(): Unit =
  Using.resource(LayaClient()) { client =>
    val state = "Hi, we were billed twice for March. Please refund the duplicate today or we will cancel our plan."

    val department = Choice.described(
      "Which department should handle this?",
      "billing"   -> "invoices, payments, refunds",
      "technical" -> "bugs, outages, system errors",
      "other"     -> "everything else"
    )
    val urgency    = Score("How urgent is this?", "not urgent", "soon", "critical")
    val churnRisk  = Noul("Does the user threaten to cancel or leave?")

    val response                                 = client.ask(state, (department, urgency, churnRisk))
    val (deptAnswer, urgencyAnswer, churnAnswer) = response.answers

    println(s"Department: ${deptAnswer.choice} (confidence ${deptAnswer.confidence})")
    println(s"Urgency score: ${urgencyAnswer.score} -> ${urgencyAnswer.mostLikely}")
    println(s"Churn risk probability: ${churnAnswer.probability}")
    println(s"Model used: ${response.model}, requestId: ${response.requestId}")
  }
