package de.wittig.pekko.bank

import munit.FunSuite
import org.apache.pekko.actor.testkit.typed.FishingOutcome
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.eventstream.EventStream

import scala.concurrent.duration.*

class BankTest extends FunSuite:

  private val testKit = ActorTestKit()

  private def assertRejected(result: Account.Result): Unit =
    result match
      case Account.Rejected(_) => ()
      case other               => fail(s"Rejected erwartet, war: $other")

  private def assertFailed(reply: Transfer.Reply): Unit =
    reply match
      case Transfer.Failed(_) => ()
      case other              => fail(s"Failed erwartet, war: $other")

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("Einzahlung erhoeht den Kontostand") {
    val account = testKit.spawn(Account("a1"))
    val probe   = testKit.createTestProbe[Account.Result]()

    account ! Account.Deposit(100, probe.ref)
    assertEquals(probe.receiveMessage(), Account.Done(100))
    account ! Account.GetBalance(probe.ref)
    assertEquals(probe.receiveMessage(), Account.Done(100))
  }

  test("Abhebung ohne Deckung wird abgelehnt") {
    val account = testKit.spawn(Account("a2"))
    val probe   = testKit.createTestProbe[Account.Result]()

    account ! Account.Withdraw(10, probe.ref)
    assertRejected(probe.receiveMessage())
  }

  test("Negative Einzahlung wird abgelehnt") {
    val account = testKit.spawn(Account("a3"))
    val probe   = testKit.createTestProbe[Account.Result]()

    account ! Account.Deposit(-5, probe.ref)
    assertRejected(probe.receiveMessage())
  }

  test("Nach einem Absturz startet das Konto mit Stand 0 neu") {
    val account = testKit.spawn(Account("a4"))
    val probe   = testKit.createTestProbe[Account.Result]()

    account ! Account.Deposit(50, probe.ref)
    probe.expectMessage(Account.Done(50))
    account ! Account.Crash
    account ! Account.GetBalance(probe.ref)
    probe.expectMessage(Account.Done(0))
  }

  test("Konto veroeffentlicht Transaktionen auf dem EventStream") {
    val events  = testKit.createTestProbe[Account.Transaction]()
    testKit.system.eventStream ! EventStream.Subscribe(events.ref)
    val account = testKit.spawn(Account("a5"))

    account ! Account.Deposit(20, testKit.system.ignoreRef)
    val seen = events.fishForMessage(3.seconds) { tx =>
      tx.accountId match
        case "a5" => FishingOutcome.Complete
        case _    => FishingOutcome.ContinueAndIgnore
    }
    assertEquals(seen.lastOption.map(_.balance), Some(BigDecimal(20)))
  }

  test("Ueberweisung zwischen zwei Konten gelingt") {
    val from  = testKit.spawn(Account("t1"))
    val to    = testKit.spawn(Account("t2"))
    val probe = testKit.createTestProbe[Account.Result]()
    val reply = testKit.createTestProbe[Transfer.Reply]()

    from ! Account.Deposit(100, probe.ref)
    probe.expectMessage(Account.Done(100))

    testKit.spawn(Transfer(from, to, 40, reply.ref))
    reply.expectMessage(Transfer.Completed)

    from ! Account.GetBalance(probe.ref)
    probe.expectMessage(Account.Done(60))
    to ! Account.GetBalance(probe.ref)
    probe.expectMessage(Account.Done(40))
  }

  test("Ueberweisung ohne Deckung schlaegt fehl und aendert nichts") {
    val from  = testKit.spawn(Account("t3"))
    val to    = testKit.spawn(Account("t4"))
    val probe = testKit.createTestProbe[Account.Result]()
    val reply = testKit.createTestProbe[Transfer.Reply]()

    testKit.spawn(Transfer(from, to, 40, reply.ref))
    assertFailed(reply.receiveMessage())

    to ! Account.GetBalance(probe.ref)
    probe.expectMessage(Account.Done(0))
  }

  test("Bank: Ueberweisung an unbekanntes Konto schlaegt fehl") {
    val bank  = testKit.spawn(Bank())
    val reply = testKit.createTestProbe[Transfer.Reply]()

    bank ! Bank.MakeTransfer("x", "y", 1, reply.ref)
    assertFailed(reply.receiveMessage())
  }

  test("Bank: eroeffnet Konten und leitet Nachrichten weiter") {
    val bank   = testKit.spawn(Bank())
    val opened = testKit.createTestProbe[Bank.Opened]()
    val probe  = testKit.createTestProbe[Account.Result]()

    bank ! Bank.Open("carol", opened.ref)
    opened.expectMessage(Bank.Opened("carol"))
    bank ! Bank.ToAccount("carol", Account.Deposit(7, probe.ref))
    probe.expectMessage(Account.Done(7))
  }
