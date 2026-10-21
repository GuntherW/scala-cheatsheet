package de.wittig.pekko.bank

import org.apache.pekko.actor.typed.eventstream.EventStream
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}

import scala.concurrent.duration.*

/** Ein Konto = ein Actor. Der Kontostand lebt nur im Behavior (kein `var`): jede Nachricht liefert das naechste Behavior zurueck. */
object Account:

  sealed trait Command
  final case class Deposit(amount: BigDecimal, replyTo: ActorRef[Result])  extends Command
  final case class Withdraw(amount: BigDecimal, replyTo: ActorRef[Result]) extends Command
  final case class GetBalance(replyTo: ActorRef[Result])                   extends Command
  case object Crash                                                        extends Command

  sealed trait Result
  final case class Done(balance: BigDecimal) extends Result
  final case class Rejected(reason: String)  extends Result

  /** Wird auf dem EventStream des ActorSystems veroeffentlicht (Publish/Subscribe). */
  final case class Transaction(accountId: String, description: String, balance: BigDecimal)

  def apply(id: String): Behavior[Command] =
    // Supervision: bei IllegalStateException wird der Actor neu gestartet -> Kontostand ist wieder 0
    Behaviors
      .supervise(Behaviors.setup[Command](ctx => active(ctx, id, BigDecimal(0))))
      .onFailure[IllegalStateException](SupervisorStrategy.restart.withLimit(3, 1.minute))

  private def active(ctx: ActorContext[Command], id: String, balance: BigDecimal): Behavior[Command] =
    Behaviors.receiveMessage {
      case Deposit(amount, replyTo) if amount <= 0 =>
        replyTo ! Rejected("Betrag muss positiv sein")
        Behaviors.same

      case Deposit(amount, replyTo) =>
        val newBalance = balance + amount
        replyTo ! Done(newBalance)
        publish(ctx, Transaction(id, s"Einzahlung $amount", newBalance))
        active(ctx, id, newBalance)

      case Withdraw(amount, replyTo) if amount <= 0 || amount > balance =>
        replyTo ! Rejected(s"Abhebung von $amount nicht moeglich (Stand: $balance)")
        Behaviors.same

      case Withdraw(amount, replyTo) =>
        val newBalance = balance - amount
        replyTo ! Done(newBalance)
        publish(ctx, Transaction(id, s"Abhebung $amount", newBalance))
        active(ctx, id, newBalance)

      case GetBalance(replyTo) =>
        replyTo ! Done(balance)
        Behaviors.same

      case Crash =>
        throw IllegalStateException(s"Konto $id ist abgestuerzt")
    }

  private def publish(ctx: ActorContext[Command], tx: Transaction): Unit =
    ctx.system.eventStream ! EventStream.Publish(tx)
