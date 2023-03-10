package de.wittig.zio.rockthejvm.stm.simpleexample
import de.wittig.zio.rockthejvm.util.debugThread
import zio.stm.{STM, TRef}
import zio.*

object NotTransactional:
  def transferMoney(sender: Ref[Long], receiver: Ref[Long], amount: Long, id: Int) =
    for
      senderBalance      <- sender.get
      _                  <- if (senderBalance < amount) ZIO.fail(s"[transactionId $id]: Insufficient funds.") else ZIO.unit
      newSenderBalance   <- sender.updateAndGet(_ - amount)
      newReceiverBalance <- receiver.updateAndGet(_ + amount)
    yield s"[transactionId]: $id, [sender] $newSenderBalance, [receiver] $newReceiverBalance"

  val exploitBuggyBank =
    for
      id       <- Random.nextIntBounded(1000)
      sender   <- Ref.make(1000L)
      receiver <- Ref.make(0L)
      fib1     <- transferMoney(sender, receiver, 1000, id).debugThread.fork
      fib2     <- transferMoney(sender, receiver, 1000, id).debugThread.fork
      _        <- fib1.zip(fib2).join
      _        <- receiver.get.debugThread
    yield ()
