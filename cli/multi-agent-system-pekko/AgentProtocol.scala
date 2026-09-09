package agents

import org.apache.pekko.actor.typed.ActorRef

import scala.util.Try

/** Nachrichtenprotokoll, das JEDER Agenten-Aktor versteht.
  *
  * Wichtiger Unterschied zum `ox`/direkten-Methodenaufruf-Ansatz in `research_scala`: Aktoren kommunizieren AUSSCHLIESSLICH über (typisierte, unveränderliche) Nachrichten - niemals über direkte
  * Methodenaufrufe oder geteilten Zustand. Ein Aktor verarbeitet eingehende Nachrichten strikt nacheinander (ein Aktor = niemals gleichzeitig zwei Nachrichten), wodurch man sich innerhalb eines
  * Aktors keine Gedanken über Data Races machen muss.
  */
object AgentProtocol:

  /** Basistyp aller Nachrichten, die ein Agenten-Aktor empfangen kann. */
  sealed trait Command

  /** Bittet den Agenten-Aktor, einen Model-Call auszuführen.
    *
    * `replyTo` ist die "Return-Adresse" - im Aktor-Modell gibt es keine Rückgabewerte von Methodenaufrufen; stattdessen schickt der antwortende Aktor eine neue Nachricht an eine ActorRef, die ihm der
    * Absender mitgegeben hat (hier: `replyTo`). Das nennt man "Request-Response über Nachrichtenaustausch".
    */
  final case class Run(userMessage: String, maxTokens: Int, replyTo: ActorRef[Result]) extends Command

  /** Interne Nachricht: Ergebnis des (asynchron ausgeführten) blockierenden HTTP-Aufrufs, siehe `AgentActor.pipeToSelf`. Wird niemals von außen geschickt, sondern nur vom Aktor selbst an sich selbst.
    */
  final private[agents] case class WrappedResponse(replyTo: ActorRef[Result], result: Try[String]) extends Command

  /** Antwort eines Agenten-Aktors auf ein `Run`. */
  final case class Result(output: String)
