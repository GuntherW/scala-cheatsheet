package rag

import org.llm4s.error.LLMError
import rag.ingestion.{GenerateSampleDocs, Ingestion}
import rag.observability.Observability
import rag.query.RagAssistant

import scala.util.CommandLineParser

/** Subcommands: `generate-docs`, `ingest`, `ask "<Frage>"`. Traces gehen per OpenTelemetry an OpenObserve (siehe observability/). */
@main def main(command: Command, rest: String*): Unit =
  val docsDir = os.pwd / "docs"

  command match
    case Command.GenerateDocs => GenerateSampleDocs.run(docsDir)
    case Command.Ingest       => withObservability(obs => Ingestion.run(docsDir, obs).left.foreach(reportError))
    case Command.Ask          => rest.headOption match
        case Some(question) => withObservability(obs => RagAssistant.ask(question, obs).fold(reportError, println))
        case None           => println("""Bitte eine Frage angeben:
                                         |  scala-cli run . -- ask "<Frage>"""".stripMargin)

private def reportError(error: LLMError): Unit = println(s"Fehler: ${ErrorReport.describe(error)}")

private def withObservability(body: Observability => Unit): Unit =
  val obs = Observability.build()
  println(s"[Observability] OpenTelemetry -> OpenObserve: ${if obs.otelActive then "an" else "aus"}")
  try body(obs)
  finally obs.shutdown()

enum Command:
  case GenerateDocs, Ingest, Ask

object Command:
  given CommandLineParser.FromString[Command] with
    def fromString(value: String): Command = value match
      case "generate-docs" => Command.GenerateDocs
      case "ingest"        => Command.Ingest
      case "ask"           => Command.Ask
      case other           => throw IllegalArgumentException(s"Unbekannter Befehl: '$other' (erwartet: generate-docs | ingest | ask)")
