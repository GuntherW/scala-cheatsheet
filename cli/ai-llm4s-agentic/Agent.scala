package agents

import org.llm4s.agent.{Agent as Llm4sAgent, AgentStatus}
import org.llm4s.llmconnect.model.{AssistantMessage, CompletionOptions}
import org.llm4s.toolapi.{ToolFunction, ToolRegistry}

/** Basisklasse für alle Agenten: Rolle (System-Prompt) + Tools. llm4s' `Agent` übernimmt den Tool-Use-Loop (führt die Tool-Handler selbst aus und iteriert, bis das Modell ohne Tool-Aufruf antwortet).
  */
abstract class Agent(name: String, systemPrompt: String, tools: Seq[ToolFunction[?, ?]] = Nil, maxSteps: Int = 10):

  private val llm4sAgent = new Llm4sAgent(Llm.client)

  protected def run(userMessage: String, maxTokens: Int = 2000): String =
    println(s"[$name] Start")
    val state = llm4sAgent
      .run(
        query = userMessage,
        tools = new ToolRegistry(tools),
        maxSteps = Some(maxSteps),
        systemPromptAddition = Some(systemPrompt),
        completionOptions = CompletionOptions(maxTokens = Some(maxTokens)),
      )
      .fold(error => throw new RuntimeException(s"[$name] LLM-Fehler: ${error.formatted}"), identity)

    state.status match
      case AgentStatus.Failed(reason) => throw new RuntimeException(s"[$name] Agent fehlgeschlagen: $reason")
      case _                          =>
        val assistant = state.conversation.messages.collect { case a: AssistantMessage => a }
        println(s"[$name] Fertig. Tool-Aufrufe: ${assistant.flatMap(_.toolCalls.map(_.name)).mkString(", ")}")
        assistant.reverseIterator
          .collectFirst { case a if a.toolCalls.isEmpty && a.content.nonEmpty => a.content }
          .getOrElse(throw new RuntimeException(s"[$name] Keine finale Antwort erhalten."))
