package de.wittig.zio.rockthejvm.stm.simpleexample

import de.wittig.zio.rockthejvm.util.debugThread
import zio.stm.{STM, TRef}
import zio.*

object Transactional:
  def transferMoney(sender: TRef[Long], receiver: TRef[Long], amount: Long, id: Int): STM[String, String] =
    for
      senderBalance      <- sender.get
      _                  <- if (senderBalance < amount) STM.fail(s"[transactionId $id]: Insufficient funds. ") else STM.unit
      newSenderBalance   <- sender.updateAndGet(_ - amount)
      newReceiverBalance <- receiver.updateAndGet(_ + amount)
    yield s"[transactionId]: $id, [sender] $newSenderBalance, [receiver] $newReceiverBalance"

  val cannotExploit =
    for
      id       <- Random.nextIntBounded(1000)
      sender   <- TRef.make(1000L).commit
      receiver <- TRef.make(0L).commit
      fib1     <- transferMoney(sender, receiver, 1000, id).commit.debugThread.fork
      fib2     <- transferMoney(sender, receiver, 1000, id).commit.debugThread.fork
      _        <- fib1.zip(fib2).join
      _        <- receiver.get.commit.debugThread // will never be printed, because one of the fibers will fail
    yield ()
