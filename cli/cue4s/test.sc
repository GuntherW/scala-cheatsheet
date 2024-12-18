//> using dep tech.neander::cue4s::latest.release

import cue4s.*

Prompts.sync.use: prompts =>

  val day = prompts
    .singleChoice("How was your day?", List("great", "okay"))
    .getOrThrow

  val work = prompts
    .text("Where do you work?").getOrThrow

  val letters = prompts
    .multiChoiceAllSelected("What are your favourite letters?", ('A' to 'F').map(_.toString).toList)
    .getOrThrow

  val letters2 = prompts
    .multiChoiceNoneSelected("What are your favourite letters?", ('A' to 'F').map(_.toString).toList)
    .getOrThrow
