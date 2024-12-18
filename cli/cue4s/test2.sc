//> using dep tech.neander::cue4s::latest.release

import cue4s.*

val validateName: String => Option[PromptError] = s =>
  Option.when(s.trim.isEmpty)(PromptError("name cannot be empty!"))

case class Attributes(
    @cue(_.text("Your name").validate(validateName))
    name: String,
    @cue(_.text("Checklist").multi("Wake up" -> true, "Grub a brush" -> true, "Put a little makeup" -> false))
    doneToday: Set[String],
    @cue(_.text("What did you have for breakfast").options("eggs", "sadness"))
    breakfast: String,
    @cue(_.text("Do you want to build a snowman?"))
    snowman: Boolean,
    @cue(_.text("How old are you?"))
    age: Int,
    @cue(_.text("What is the value of PI?"))
    pi: Float
) derives PromptChain

val attributes: Attributes =
  Prompts.sync.use: p =>
    p.run(PromptChain[Attributes]).getOrThrow
