package de.wittig.yaes

import in.rcard.yaes.Output
import in.rcard.yaes.Raise
import in.rcard.yaes.Raise.*
import in.rcard.yaes.Random

/** Mehrere Effekte gleichzeitig – der zentrale Vorteil von YAES
  *
  * In klassischem Code werden Fehlerbehandlung, Logging, Random etc. durch ineinander verschachtelte Typen (IO[Either[E, A]]) oder ungetypte Exceptions ausgedrückt.
  *
  * YAES ermöglicht direkten Code, bei dem alle Effekte im Typ stehen – kompositionierbar, inkrementell handhabbar und testbar.
  *
  * Kernmechanismus: Jeder Handler löst genau einen Effekt auf. Der Rest bleibt im Typ, bis auch er behandelt wird.
  */
object ComposedEffects:

  // ─── Domäne ───────────────────────────────────────────────────────────────

  case class Order(id: Int, userId: Int, amount: Double)
  case class UserInfo(id: Int, name: String, creditLimit: Double)

  enum OrderError:
    case UserNotFound(userId: Int)
    case InsufficientCredit(need: Double, have: Double)
    case PaymentFailed(reason: String)

  // ─── Effektive Funktionen ─────────────────────────────────────────────────
  // Jede Funktion dokumentiert ihre Effekte im Typ

  def loadUser(userId: Int)(using Raise[OrderError]): UserInfo =
    if userId == 99 then Raise.raise(OrderError.UserNotFound(userId))
    else UserInfo(userId, s"User-$userId", 500.0)

  def loadOrder(orderId: Int): Order =
    Order(orderId, orderId % 10, orderId * 49.99)

  def processPayment(order: Order, user: UserInfo)(using Random, Raise[OrderError]): Boolean =
    if order.amount > user.creditLimit then
      Raise.raise(OrderError.InsufficientCredit(order.amount, user.creditLimit))
    val success = Random.nextBoolean
    if !success then Raise.raise(OrderError.PaymentFailed("Zahlung abgelehnt"))
    true

  // ─── Orchestrierung: alle Effekte komponiert ──────────────────────────────
  // Rückgabetyp zeigt: Random, Raise, Output – kein Geheimnis, alles sichtbar

  def processOrder(orderId: Int)(using Random, Raise[OrderError], Output): Boolean =
    Output.printLn(s"  Starte Bestellung $orderId ...")
    val order  = loadOrder(orderId)
    val user   = loadUser(order.userId)
    Output.printLn(s"  ${user.name} bestellt ${order.amount}€")
    val result = processPayment(order, user)
    Output.printLn(s"  Bezahlung: OK")
    result

  // ─── Schrittweises Auflösen der Effekte ───────────────────────────────────
  // Jeder Handler löst genau einen Effekt auf – der Rest bleibt im Typ

  // Schritt 1: Output auflösen → Raise, Random bleiben
  def withoutOutput(orderId: Int): (Random, Raise[OrderError]) ?=> Boolean =
    Output.run(processOrder(orderId))

  // Schritt 2: Random auflösen → nur noch Raise bleibt
  def withoutRandom(orderId: Int): Raise[OrderError] ?=> Boolean =
    Random.run(withoutOutput(orderId))

  // Schritt 3: alle Effekte auflösen → reiner Either-Wert
  def runOrder(orderId: Int): Either[OrderError, Boolean] =
    Raise.either(withoutRandom(orderId))

  // Pipeline für mehrere Bestellungen
  def runPipeline(orderIds: List[Int]): List[Either[OrderError, Boolean]] =
    orderIds.map(runOrder)

  // ─── MapError: Fehler zwischen Layern konvertieren ────────────────────────

  enum ApiError:
    case BadRequest(msg: String)
    case ServerError(msg: String)

  def handleOrder(orderId: Int)(using Random, Output, Raise[ApiError]): Boolean =
    given MapError[OrderError, ApiError] = MapError {
      case OrderError.UserNotFound(id)         => ApiError.BadRequest(s"User $id nicht gefunden")
      case OrderError.InsufficientCredit(n, h) => ApiError.BadRequest(s"Kredit zu niedrig: ${n}€ > ${h}€")
      case OrderError.PaymentFailed(r)         => ApiError.ServerError(s"Zahlung fehlgeschlagen: $r")
    }
    processOrder(orderId)

  // ─── Demo ──────────────────────────────────────────────────────────────────

  @main
  def runComposedEffects(): Unit =
    println("=== Composed Effects Demo ===\n")

    // 1. Pipeline
    println("1) Pipeline mit mehreren Bestellungen:\n")
    val results    = runPipeline(List(1, 2, 3, 4, 5))
    println("\nErgebnisse:")
    results.zipWithIndex.foreach { (r, i) =>
      println(s"  Bestellung ${i + 1}: $r")
    }
    val (ok, fail) = results.partition(_.isRight)
    println(s"\nErfolgreich: ${ok.size}, Fehlgeschlagen: ${fail.size}\n")

    // 2. Schrittweises Auflösen zeigen
    println("2) Schrittweises Auflösen der Effekte:")
    val step1: (Random, Raise[OrderError]) ?=> Boolean = withoutOutput(1)
    val step2: Raise[OrderError] ?=> Boolean           = Random.run(step1)
    val step3: Either[OrderError, Boolean]             = Raise.either(step2)
    println(s"   Nach Output.run:  (Random, Raise) ?=> Boolean")
    println(s"   Nach Random.run:  Raise ?=> Boolean")
    println(s"   Nach Raise.either: $step3\n")

    // 3. Layer-Mapping
    println("3) Fehler-Mapping (OrderError → ApiError):")
    val apiResult: Either[ApiError, Boolean] = Raise.either {
      Output.run {
        Random.run {
          handleOrder(99) // userId 99 → UserNotFound
        }
      }
    }
    println(s"   API-Fehler: $apiResult")
