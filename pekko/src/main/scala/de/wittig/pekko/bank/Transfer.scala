package de.wittig.pekko.bank

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.*

/** Kurzlebiger Actor pro Ueberweisung ("Saga"): abheben -> einzahlen -> ggf. zurueckbuchen. Beendet sich danach selbst. */
object Transfer:

  sealed trait Reply
  case object Completed                   extends Reply
  final case class Failed(reason: String) extends Reply

  sealed trait Command
  final private case class AccountReply(result: Account.Result) extends Command
  private case object TimedOut                                  extends Command

  def apply(from: ActorRef[Account.Command], to: ActorRef[Account.Command], amount: BigDecimal, replyTo: ActorRef[Reply]): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.setReceiveTimeout(2.seconds, TimedOut)
      val adapter = ctx.messageAdapter[Account.Result](AccountReply.apply)

      def fail(reason: String): Behavior[Command] =
        replyTo ! Failed(reason)
        Behaviors.stopped

      def awaitingDeposit: Behavior[Command] =
        Behaviors.receiveMessage {
          case AccountReply(Account.Done(_))          =>
            replyTo ! Completed
            Behaviors.stopped
          case AccountReply(Account.Rejected(reason)) =>
            from ! Account.Deposit(amount, ctx.system.ignoreRef) // Rueckbuchung, Antwort egal
            fail(s"Einzahlung fehlgeschlagen: $reason")
          case TimedOut                               => fail("Timeout bei der Einzahlung")
        }

      def awaitingWithdraw: Behavior[Command] =
        Behaviors.receiveMessage {
          case AccountReply(Account.Done(_))          =>
            to ! Account.Deposit(amount, adapter)
            awaitingDeposit
          case AccountReply(Account.Rejected(reason)) => fail(s"Abhebung fehlgeschlagen: $reason")
          case TimedOut                               => fail("Timeout bei der Abhebung")
        }

      from ! Account.Withdraw(amount, adapter)
      awaitingWithdraw
    }
