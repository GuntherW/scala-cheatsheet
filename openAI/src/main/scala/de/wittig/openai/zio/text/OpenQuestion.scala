package de.wittig.openai.zio.text

import zio.openai.*
import zio.openai.model.CreateCompletionRequest.Model.Models
import zio.openai.model.CreateCompletionRequest.{MaxTokens, Model, Prompt}
import zio.openai.model.Temperature
import zio.{Console, ZIO, ZIOAppDefault}

object OpenQuestion extends ZIOAppDefault {

  private def loop = for {
    question <- Console.readLine("Frage: ")
    result   <- Completions.createCompletion(
                  model = Model.Predefined(Models.`Davinci-002`),
                  prompt = Prompt.String(question),
                  maxTokens = MaxTokens(640),
                  temperature = Temperature(0.9)
                )
    _        <- Console.printLine("Antwort: " + result.choices.map(_.text).mkString(", "))
  } yield ()

  override def run = loop.forever.provide(Completions.default)
}
