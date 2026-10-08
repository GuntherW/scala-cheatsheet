package rag.query

import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.chat.ChatModel

/** Zentrale LLM-Konfiguration für die Antwortgenerierung. Dieses Projekt spricht (wie die `ai-sttpai-*`-Projekte) einen requesty.ai-Router statt der offiziellen Anthropic-Basis-URL an -
  * `AnthropicChatModel.builder().baseUrl(...)` unterstützt das direkt. Der Requesty-Key wird als `apiKey` übergeben (Requesty akzeptiert ihn anstelle eines echten Anthropic-Keys).
  *
  * Der Key wird direkt aus `sys.env("ANTHROPIC_API_KEY")` gelesen (keine eigene `.env`-Datei-Logik, siehe `PgConfig.scala`) - ist die Variable nicht gesetzt, bricht der Zugriff mit einer klaren
  * `NoSuchElementException`-Meldung samt Variablennamen ab.
  */
object LlmConfig:

  // Anders als sttp-ai (das selbst "v1/messages" anhängt) erwartet langchain4j's AnthropicChatModel bereits ein "/v1" am Ende der baseUrl und hängt nur noch "messages" an.
  val baseUrl: String = "https://router.eu.requesty.ai/v1"

  /** `claude.sonnet`/`claude-sonnet-5` lehnt dieser Router ab - benötigt wird die Vertex-Modell-Id, siehe auch `ai-sttpai-agentic`/`ai-orca-agentic`. */
  val model: String = "vertex/claude-sonnet-5-5@eu"

  def chatModel(): ChatModel =
    AnthropicChatModel
      .builder()
      .baseUrl(baseUrl)
      .apiKey(sys.env("ANTHROPIC_API_KEY"))
      .modelName(model)
      .build()
