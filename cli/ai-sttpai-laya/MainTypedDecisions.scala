package laya

import sttp.ai.jev.{Choice, Noul, Score}

import scala.util.Using

/** Wie Main.scala, aber gegen den "typed-decisions"-Checkpoint von Laya - das für typisierte Entscheidungs-Workflows feingetunte Modell (laut Laya-README auf dem typed-decisions-Benchmark deutlich
  * genauer als die zero-shot "english"/"multilingual"-Checkpoints).
  *
  * Voraussetzung: `laya-serve` läuft mit `LAYA_MODELS` enthält "typed-decisions" (siehe README.md).
  *
  * Aufruf:
  * {{{
  *   scala-cli run . --main-class laya.mainTypedDecisions
  * }}}
  */
@main def mainTypedDecisions(): Unit =
  Using.resource(LayaClient("typed-decisions")) { client =>
    val state = "Customer: The app crashes every time I try to upload a photo larger than 5MB. This started after yesterday's update."

    val severity        = Score(
      "How severe is this problem?",
      "minor: cosmetic or rare",
      "moderate: a feature is broken but there is a workaround",
      "major: blocks a core feature for many users"
    )
    val needsEscalation = Noul("Should this be escalated to the engineering team immediately?")

    val response                           = client.ask(state, (severity, needsEscalation))
    val (severityAnswer, escalationAnswer) = response.answers

    println(s"Severity score: ${severityAnswer.score} -> ${severityAnswer.mostLikely}")
    println(s"Needs escalation probability: ${escalationAnswer.probability}")
    println(s"Model used: ${response.model}, requestId: ${response.requestId}")
  }
