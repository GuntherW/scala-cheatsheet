package agents

import org.llm4s.agent.{Agent as Llm4sAgent, AgentStatus}
import org.llm4s.llmconnect.model.{AssistantMessage, CompletionOptions}
import org.llm4s.toolapi.{ToolFunction, ToolRegistry}

/** Basisklasse für alle Agenten: Rolle (System-Prompt) + Tools. llm4s' `Agent` übernimmt den Tool-Use-Loop (führt die Tool-Handler selbst aus und iteriert, bis das Modell ohne Tool-Aufruf antwortet).
  *
  * llm4s 0.5.1: Der Agent wird nicht mehr per `new Agent(client)` konstruiert, sondern mit `Agent.builder(id, client)` gebaut. System-Prompt, Tools, `maxSteps` und `CompletionOptions` sind
  * Builder-Einstellungen (nicht mehr Argumente von `run`).
  */
abstract class Agent(
  name: String,
  systemPrompt: String,
  tools: Seq[ToolFunction[?, ?]] = Nil,
  maxSteps: Int = 10,
  maxTokens: Int = 2000,
):

  private val llm4sAgent: Llm4sAgent =
    Llm4sAgent
      .builder(id = name, client = Llm.client)
      .withSystemPrompt(systemPrompt)
      .withTools(new ToolRegistry(tools))
      .withMaxSteps(maxSteps)
      .withCompletionOptions(CompletionOptions(maxTokens = Some(maxTokens)))
      .build()
      .fold(error => throw new RuntimeException(s"[$name] Agent konnte nicht gebaut werden: ${error.formatted}"), identity)

  protected def run(userMessage: String): String =
    println(s"[$name] Start")
    val result = llm4sAgent
      .run(userMessage)
      .fold(error => throw new RuntimeException(s"[$name] LLM-Fehler: ${error.formatted}"), identity)

    result.status match
      case AgentStatus.Blocked(guardrail, reason) => throw new RuntimeException(s"[$name] Agent durch Guardrail '$guardrail' blockiert: $reason")
      case _                                      =>
        val assistant = result.messages.collect { case a: AssistantMessage => a }
        println(s"[$name] Fertig. Tool-Aufrufe: ${assistant.flatMap(_.toolCalls.map(_.name)).mkString(", ")}")
        assistant.reverseIterator
          .collectFirst { case a if a.toolCalls.isEmpty && a.content.nonEmpty => a.content }
          .getOrElse(throw new RuntimeException(s"[$name] Keine finale Antwort erhalten."))
