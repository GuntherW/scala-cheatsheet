package rag

import rag.ingestion.{GenerateSampleDocs, Ingestion}
import rag.query.RagAssistant

import scala.util.CommandLineParser

@main def main(command: Command, rest: String*): Unit =
  val docsDir = os.pwd / "docs"

  command match
    case Command.GenerateDocs => GenerateSampleDocs.run(docsDir)
    case Command.Ingest       => Ingestion.run(docsDir).left.foreach(e => println(s"Fehler: ${e.formatted}"))
    case Command.Ask          => rest.headOption match
        case Some(question) => RagAssistant.ask(question).fold(e => println(s"Fehler: ${e.formatted}"), println)
        case None           => println("""Bitte eine Frage angeben:
                                         |  scala-cli run . -- ask "<Frage>"""".stripMargin)

enum Command:
  case GenerateDocs, Ingest, Ask

object Command:
  given CommandLineParser.FromString[Command] with
    def fromString(value: String): Command = value match
      case "generate-docs" => Command.GenerateDocs
      case "ingest"        => Command.Ingest
      case "ask"           => Command.Ask
      case other           => throw IllegalArgumentException(s"Unbekannter Befehl: '$other' (erwartet: generate-docs | ingest | ask)")
