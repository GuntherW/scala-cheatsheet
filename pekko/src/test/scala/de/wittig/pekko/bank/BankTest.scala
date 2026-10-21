package de.wittig.pekko.bank

import munit.FunSuite
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit

class BankTest extends FunSuite:

  private val testKit = ActorTestKit()

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
    assert(probe.receiveMessage().isInstanceOf[Account.Rejected])
  }

  test("Negative Einzahlung wird abgelehnt") {
    val account = testKit.spawn(Account("a3"))
    val probe   = testKit.createTestProbe[Account.Result]()

    account ! Account.Deposit(-5, probe.ref)
    assert(probe.receiveMessage().isInstanceOf[Account.Rejected])
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
    testKit.system.eventStream ! org.apache.pekko.actor.typed.eventstream.EventStream.Subscribe(events.ref)
    val account = testKit.spawn(Account("a5"))

    account ! Account.Deposit(20, testKit.system.ignoreRef)
    assertEquals(events.receiveMessage().balance, BigDecimal(20))
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
    assert(reply.receiveMessage().isInstanceOf[Transfer.Failed])

    to ! Account.GetBalance(probe.ref)
    probe.expectMessage(Account.Done(0))
  }

  test("Bank: Ueberweisung an unbekanntes Konto schlaegt fehl") {
    val bank  = testKit.spawn(Bank())
    val reply = testKit.createTestProbe[Transfer.Reply]()

    bank ! Bank.MakeTransfer("x", "y", 1, reply.ref)
    assert(reply.receiveMessage().isInstanceOf[Transfer.Failed])
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
