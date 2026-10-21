package de.wittig.pekko.bank

import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler}
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

/** Ablauf: Konten eroeffnen, ein-/auszahlen, ueberweisen, Absturz + Neustart eines Kontos (Supervision) demonstrieren. */
@main def bankApp(): Unit =
  given system: ActorSystem[Bank.Command] = ActorSystem(Bank(), "bank")
  given Timeout                           = 3.seconds
  given Scheduler                         = system.scheduler
  import system.executionContext

  def balance(id: String): Future[Account.Result] = system.ask(replyTo => Bank.ToAccount(id, Account.GetBalance(replyTo)))

  val program =
    for
      _  <- system.ask[Bank.Opened](Bank.Open("alice", _))
      _  <- system.ask[Bank.Opened](Bank.Open("bob", _))
      d  <- system.ask[Account.Result](r => Bank.ToAccount("alice", Account.Deposit(100, r)))
      _   = println(s"Einzahlung alice: $d")
      w  <- system.ask[Account.Result](r => Bank.ToAccount("bob", Account.Withdraw(50, r)))
      _   = println(s"Abhebung bob (leer): $w")
      t1 <- system.ask[Transfer.Reply](Bank.MakeTransfer("alice", "bob", 30, _))
      _   = println(s"Ueberweisung 30: $t1")
      t2 <- system.ask[Transfer.Reply](Bank.MakeTransfer("alice", "bob", 500, _))
      _   = println(s"Ueberweisung 500: $t2")
      a  <- balance("alice")
      b  <- balance("bob")
      _   = println(s"Staende: alice=$a, bob=$b")
      _   = system ! Bank.ToAccount("bob", Account.Crash)
      b2 <- balance("bob")
      _   = println(s"bob nach Absturz (Neustart, Stand zurueckgesetzt): $b2")
    yield ()

  try Await.result(program, 15.seconds)
  finally
    system.terminate()
    Await.result(system.whenTerminated, 10.seconds)
