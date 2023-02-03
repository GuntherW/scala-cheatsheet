package de.wittig.openai.text

import zio.openai.*
import zio.openai.model.CreateCompletionRequest.Prompt
import zio.openai.model.Temperature
import zio.{Console, ZIO, ZIOAppDefault}

object OpenQuestion extends ZIOAppDefault {

  private def loop = for {
    question <- Console.readLine("Frage: ")
    result   <- Completions.createCompletion(
                  model = "text-davinci-003",
                  prompt = Prompt.String(question),
                  temperature = Temperature(0.9)
                )
    _        <- Console.printLine("Antwort: " + result.choices.flatMap(_.text.toOption).mkString(", "))
  } yield ()

  override def run = loop.forever.provide(Completions.default)
}
