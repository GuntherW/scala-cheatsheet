package agents

import org.llm4s.config.Llm4sConfig
import org.llm4s.llmconnect.LLMClient
import org.llm4s.llmconnect.config.{ContextWindowResolver, OpenAIConfig}
import org.llm4s.llmconnect.provider.OpenRouterClient
import org.llm4s.model.ModelRegistryService

/** Baut den llm4s-`LLMClient` für den Requesty-Router (EU, OpenAI-kompatible API).
  *
  * Bewusst NICHT über `LLMConnect`/`application.conf`, sondern direkt als `OpenRouterClient` (llm4s 0.4.1):
  *   - Provider `openai`/`requesty` nutzen das Azure-SDK und bauen Azure-Pfade (`/openai/deployments/<model>/chat/completions`) -> 404 beim Router.
  *   - Provider `anthropic` verwirft Assistant-Nachrichten mit Tool-Calls und schickt Tool-Ergebnisse als einfachen User-Text -> das Modell ruft das Tool endlos erneut auf.
  *   - `OpenRouterClient` postet schlicht an `${baseUrl}/chat/completions` im OpenAI-Format (inkl. korrekter Tool-Calls) und funktioniert damit mit jedem OpenAI-kompatiblen Endpunkt.
  */
object Llm:

  private val BaseUrl = "https://router.eu.requesty.ai/v1"
  private val Model   = "vertex/claude-sonnet-5-5@eu"

  private val apiKey: String = sys.env("ANTHROPIC_API_KEY")

  val client: LLMClient =
    val created = for
      registry <- Llm4sConfig.modelRegistryService()
      given ModelRegistryService = registry
      given ContextWindowResolver = ContextWindowResolver(registry)
      client <- OpenRouterClient(OpenAIConfig.fromValues(Model, apiKey, None, BaseUrl))
    yield client
    created.fold(error => throw new RuntimeException(s"LLM-Client konnte nicht erstellt werden: ${error.formatted}"), identity)
