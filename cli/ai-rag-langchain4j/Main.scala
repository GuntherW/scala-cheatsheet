package rag

import rag.ingestion.{GenerateSampleDocs, Ingestion}
import rag.query.RagAssistant

import scala.util.CommandLineParser

/** Einstiegspunkt dieser Lernpipeline - drei Subcommands:
  *   - `generate-docs`: erzeugt die 10 Beispiel-PDFs in `docs/` (einmalig, oder erneut bei geänderten Inhalten in `ingestion/SampleDocs.scala`).
  *   - `ingest`: liest alle PDFs aus `docs/`, chunked/embedded sie und schreibt sie in pgvector (siehe `ingestion/Ingestion.scala`). Leert vorher die Tabelle, mehrfaches Ausführen ist also gefahrlos.
  *   - `ask "<Frage>"`: beantwortet eine Frage auf Basis der zuvor indexierten Dokumente (siehe `query/RagAssistant.scala`).
  *
  * Voraussetzung für `ingest`/`ask`: der `postgres-rag`-Container läuft (`cd docker && docker compose up -d postgres-rag`).
  */
@main def main(command: Command, rest: String*): Unit =
  val docsDir = os.pwd / "docs"

  command match
    case Command.GenerateDocs => GenerateSampleDocs.run(docsDir)
    case Command.Ingest       => Ingestion.run(docsDir)
    case Command.Ask          => rest.headOption match
        case Some(question) => println(RagAssistant.ask(question))
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
