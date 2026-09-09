package agents

import org.apache.pekko.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import AnthropicModels.ClientTool
import scala.util.{Failure, Success}
import scala.concurrent.Future

/** Fabrik für das Verhalten (`Behavior`) eines einzelnen Agenten-Aktors.
  *
  * Entspricht fachlich der `abstract class Agent` aus `research_scala`, ist hier aber ein `Behavior[AgentProtocol.Command]` statt einer Klasse mit einer normalen Methode `run(...)`. Ein Aktor hat
  * KEINE öffentlichen Methoden - die einzige Schnittstelle nach außen ist sein Postfach (Mailbox), an das man Nachrichten seines Protokolltyps schickt.
  *
  * '''Blockierendes I/O im Aktor-Modell:''' Der eigentliche HTTP-Aufruf an die Anthropic-API (`AnthropicClient.chat` / `chatWithTool`) ist blockierend. Würde man ihn direkt im
  * `receiveMessage`-Handler ausführen, würde er den Thread blockieren, der eigentlich für (potenziell viele) andere Aktoren im selben Dispatcher-Pool zuständig ist - ein klassischer
  * Pekko/Akka-Fallstrick. Die Lösung: Der Aufruf läuft in einem separaten `Future` auf einem EIGENEN, dafür vorgesehenen Dispatcher (`blocking-io-dispatcher`, siehe `resources/application.conf`), und
  * das Ergebnis wird über `context.pipeToSelf` als normale Nachricht an den Aktor selbst zurückgemeldet. So bleibt der Standard-Dispatcher frei für andere Aktoren, während der Netzwerk-Call läuft.
  */
object AgentActor:
  import AgentProtocol.*

  def apply(
      name: String,
      systemPrompt: String,
      useWebSearch: Boolean = false,
      clientTools: List[ClientTool] = Nil,
      toolHandlers: Map[String, ujson.Value => String] = Map.empty,
      model: String = "vertex/claude-sonnet-5@eu",
  ): Behavior[Command] =
    Behaviors.setup { context =>
      // Eigener Dispatcher fuer blockierendes I/O statt des Standard-
      // Dispatchers, auf dem sonst die Aktor-Verarbeitung selbst laeuft.
      given blockingEc: scala.concurrent.ExecutionContext = context.system.dispatchers.lookup(DispatcherSelector.fromConfig("blocking-io-dispatcher"))

      def callModel(userMessage: String, maxTokens: Int): String =
        if clientTools.nonEmpty then
          AnthropicClient.chatWithTool(
            model = model,
            systemPrompt = systemPrompt,
            userMessage = userMessage,
            tools = clientTools,
            toolHandlers = toolHandlers,
            maxTokens = maxTokens,
          )
        else
          AnthropicClient.chat(
            model = model,
            systemPrompt = systemPrompt,
            userMessage = userMessage,
            useWebSearch = useWebSearch,
            maxTokens = maxTokens,
          )

      Behaviors.receiveMessage {
        case Run(userMessage, maxTokens, replyTo) =>
          context.log.info(s"[$name] Starte Model-Call (asynchron auf blocking-io-dispatcher)")
          context.pipeToSelf(Future(callModel(userMessage, maxTokens))) {
            case Success(output) => WrappedResponse(replyTo, Success(output))
            case Failure(ex)     => WrappedResponse(replyTo, Failure(ex))
          }
          Behaviors.same

        case WrappedResponse(replyTo, Success(output)) =>
          replyTo ! Result(output)
          Behaviors.same

        case WrappedResponse(replyTo, Failure(ex)) =>
          context.log.error(s"[$name] Fehler beim Model-Call", ex)
          replyTo ! Result(s"FEHLER im Agenten '$name': ${ex.getMessage}")
          Behaviors.same
      }
    }
