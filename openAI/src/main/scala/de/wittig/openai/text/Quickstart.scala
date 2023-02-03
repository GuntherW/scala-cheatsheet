package de.wittig.openai.text

import zio.openai.*
import zio.openai.model.CreateCompletionRequest.Prompt
import zio.openai.model.Temperature
import zio.{Console, ZIO, ZIOAppDefault}

object Quickstart extends ZIOAppDefault {

  private def generatePrompt(animal: String): Prompt = Prompt.String {
    s"""Suggest three names for an animal that is a superhero.
         |
         |Animal: Cat
         |Names: Captain Sharpclaw, Agent Fluffball, The Incredible Feline
         |Animal: Dog
         |Names: Ruff the Protector, Wonder Canine, Sir Barks-a-Lot
         |Animal: ${animal.capitalize}
         |Names:""".stripMargin
  }

  private def loop = for {
    animal <- Console.readLine("Animal: ")
    result <- Completions.createCompletion(
                model = "text-davinci-003",
                prompt = generatePrompt(animal),
                temperature = Temperature(0.6)
              )
    _      <- Console.printLine("Names: " + result.choices.flatMap(_.text.toOption).mkString(", "))
  } yield ()

  override def run = loop.forever.provide(Completions.default)
}
