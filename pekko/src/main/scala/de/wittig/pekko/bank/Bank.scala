package de.wittig.pekko.bank

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

/** Verwaltet die Konten als Kind-Actors und leitet Nachrichten weiter. */
object Bank:

  sealed trait Command
  final case class Open(id: String, replyTo: ActorRef[Opened])                                                   extends Command
  final case class ToAccount(id: String, message: Account.Command)                                               extends Command
  final case class MakeTransfer(from: String, to: String, amount: BigDecimal, replyTo: ActorRef[Transfer.Reply]) extends Command

  final case class Opened(id: String)

  final private case class AccountStopped(id: String) extends Command

  private val ValidId = "[A-Za-z0-9_-]+".r

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.spawn(Auditor(), "auditor")
      running(Map.empty)
    }

  private def running(accounts: Map[String, ActorRef[Account.Command]]): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match
        case Open(id, _) if !ValidId.matches(id) =>
          ctx.log.warn("Ungueltige Konto-ID '{}'", id)
          Behaviors.same

        case AccountStopped(id) =>
          ctx.log.warn("Konto {} wurde gestoppt", id)
          running(accounts - id)

        case Open(id, replyTo) =>
          accounts.get(id) match
            case Some(_) =>
              replyTo ! Opened(id)
              Behaviors.same
            case None    =>
              val ref = ctx.spawn(Account(id), s"account-$id")
              ctx.watchWith(ref, AccountStopped(id))
              replyTo ! Opened(id)
              running(accounts + (id -> ref))

        case ToAccount(id, message) =>
          accounts.get(id) match
            case Some(ref) => ref ! message
            case None      => ctx.log.warn("Unbekanntes Konto {}", id)
          Behaviors.same

        case MakeTransfer(from, to, amount, replyTo) =>
          (accounts.get(from), accounts.get(to)) match
            case (Some(_), Some(_)) if from.equals(to) => replyTo ! Transfer.Failed("Quelle und Ziel sind identisch")
            case (Some(f), Some(t))                    => ctx.spawnAnonymous(Transfer(f, t, amount, replyTo))
            case _                                     => replyTo ! Transfer.Failed("Unbekanntes Konto")
          Behaviors.same
    }
