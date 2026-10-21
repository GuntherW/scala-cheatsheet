package de.wittig.pekko.bank

import org.apache.pekko.actor.typed.eventstream.EventStream
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.Behavior

import scala.concurrent.duration.*

/** Abonniert den EventStream und protokolliert alle Transaktionen. Ein Timer erzeugt periodisch eine Zusammenfassung. */
object Auditor:

  sealed trait Command
  final private case class Audited(tx: Account.Transaction) extends Command
  private case object Tick                                  extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      // messageAdapter: uebersetzt fremde Nachrichtentypen in das eigene Command-Protokoll
      val adapter = ctx.messageAdapter[Account.Transaction](Audited.apply)
      ctx.system.eventStream ! EventStream.Subscribe(adapter)

      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(Tick, 1.second)
        counting(0L)
      }
    }

  private def counting(count: Long): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match
        case Audited(tx) =>
          ctx.log.info("[Audit] {}: {} -> Stand {}", tx.accountId, tx.description, tx.balance)
          counting(count + 1)
        case Tick        =>
          ctx.log.info("[Audit] Zwischenstand: {} Transaktionen", count)
          Behaviors.same
    }
