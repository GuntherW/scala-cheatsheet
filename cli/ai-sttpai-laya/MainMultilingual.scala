package laya

import sttp.ai.jev.{Choice, Noul}

import scala.util.Using

/** Wie Main.scala, aber gegen den "multilingual"-Checkpoint von Laya - z. B. für nicht-englische Support-Tickets. Laya erkennt die Sprache nicht automatisch (das übernimmt sonst Layas eigener
  * `Router`); hier wird das Modell bewusst explizit über `LayaClient("multilingual")` ausgewählt.
  *
  * Voraussetzung: `laya-serve` läuft mit `LAYA_MODELS` enthält "multilingual" (siehe README.md).
  *
  * Aufruf:
  * {{{
  *   scala-cli run . --main-class laya.mainMultilingual
  * }}}
  */
@main def mainMultilingual(): Unit =
  Using.resource(LayaClient("multilingual")) { client =>
    val state = "Hallo, wir wurden im März zweimal belastet. Bitte erstatten Sie die doppelte Abbuchung umgehend zurück, sonst kündigen wir unseren Vertrag."

    val department = Choice.described(
      "Which department should handle this?",
      "billing"   -> "invoices, payments, refunds",
      "technical" -> "bugs, outages, system errors",
      "other"     -> "everything else"
    )
    val churnRisk  = Noul("Does the user threaten to cancel or leave?")

    val response                  = client.ask(state, (department, churnRisk))
    val (deptAnswer, churnAnswer) = response.answers

    println(s"Department: ${deptAnswer.choice} (confidence ${deptAnswer.confidence})")
    println(s"Churn risk probability: ${churnAnswer.probability}")
    println(s"Model used: ${response.model}, requestId: ${response.requestId}")
  }
