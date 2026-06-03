package de.wittig.yaes

import in.rcard.yaes.Sync
import in.rcard.yaes.Raise
import in.rcard.yaes.Raise.*
import in.rcard.yaes.Output

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/** Das Sync-Effekt-System: Side-Effects explizit tracken
  *
  * Sync macht unkontrollierte Seiteneffekte (Exceptions, I/O) explizit im Typ:
  *   - Funktionen ohne Sync sind garantiert rein
  *   - Exceptions werden abgefangen statt still zu propagieren
  *   - Virtual Threads out-of-the-box (Java 21+)
  *
  * Hinweis: Der Async-Effekt (Structured Concurrency) benötigt eine Java-Version deren StructuredTaskScope-Preview-API mit der YAES-Version übereinstimmt.
  */
object SyncEffect:

  case class User(id: Int, name: String)

  // ─── Sync macht Exception-Risiken sichtbar ────────────────────────────────

  // Ohne Sync: Exception ist unsichtbar – der Typ lügt über das Verhalten
  def loadUserUnsafe(id: Int): User =
    if id <= 0 then throw new IllegalArgumentException(s"Ungültige ID: $id")
    User(id, s"User-$id")

  // Mit Sync: der Typ dokumentiert, dass dieser Aufruf schiefgehen kann
  def loadUserSafe(id: Int)(using Sync): User =
    if id <= 0 then throw new IllegalArgumentException(s"Ungültige ID: $id")
    User(id, s"User-$id")

  // ─── Sync.run: non-blocking, gibt Future zurück ────────────────────────────
  def runNonBlocking(): Unit =
    val futureUser: Future[User] = Sync.run(loadUserSafe(42))
    val user                     = Await.result(futureUser, 5.seconds)
    println(s"   Non-blocking: $user")

  // ─── Sync.runBlocking: blockiert den aktuellen Thread ─────────────────────
  def runBlocking(): Unit =
    val tryUser: scala.util.Try[User] = Sync.runBlocking(5.seconds)(loadUserSafe(7))
    println(s"   Blocking:     ${tryUser.get}")

  // ─── Sync + Raise kombiniert: effektive Fehlerbehandlung ─────────────────
  // Raise muss außerhalb von Sync.runBlocking aufgelöst werden,
  // weil runBlocking einen neuen Scope öffnet

  enum LoadError:
    case InvalidId(id: Int)
    case NetworkError(msg: String)

  def loadWithRaise(id: Int)(using Raise[LoadError]): User =
    if id <= 0 then Raise.raise(LoadError.InvalidId(id))
    else User(id, s"User-$id")

  // ─── Demo ──────────────────────────────────────────────────────────────────

  @main
  def runSyncEffect(): Unit =
    println("=== Sync Effect Demo ===\n")

    // 1. Non-blocking (Future)
    println("1) Sync.run (non-blocking, Virtual Thread):")
    runNonBlocking()
    println()

    // 2. Blocking
    println("2) Sync.runBlocking:")
    runBlocking()
    println()

    // 3. Sync + Raise
    println("3) Sync + Raise kombiniert:")
    val ok: Either[LoadError, User]  = Raise.either(loadWithRaise(5))
    val err: Either[LoadError, User] = Raise.either(loadWithRaise(-1))
    println(s"   Erfolg: $ok")
    println(s"   Fehler: $err")
    println()

    // 4. Output mit Sync
    println("4) Sync.run + Output kombiniert:")
    val result: Future[Unit] = Sync.run {
      Output.run {
        Output.printLn("   Sync-Effekt läuft in Virtual Thread")
        Output.printLn(s"   Thread: ${Thread.currentThread().isVirtual}")
      }
    }
    Await.result(result, 5.seconds)
