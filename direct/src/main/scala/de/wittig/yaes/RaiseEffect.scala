package de.wittig.yaes

import scala.language.implicitConversions
import in.rcard.yaes.Raise
import in.rcard.yaes.Raise.*

/** Das Raise-Effekt-System: typsichere Fehlerbehandlung ohne Exceptions
  *
  * Vorteile gegenüber Exceptions:
  *   - Fehler sind im Typ sichtbar – der Compiler zwingt zur Behandlung
  *   - Kein try/catch-Boilerplate
  *   - Mehrere Fehlertypen gleichzeitig trackbar
  *   - Kein Wrapping in IO/Either erforderlich
  */
object RaiseEffect:

  // ─── Domänen-Fehlertypen ───────────────────────────────────────────────────

  enum ValidationError:
    case EmptyName(field: String)
    case NegativeAge(age: Int)
    case UnknownCountry(country: String)

  case class User(name: String, age: Int, country: String)

  // ─── Effektive Funktionen ──────────────────────────────────────────────────

  // Der Fehlertyp steht im Typ: kein Aufrufer kann die Fehlermöglichkeit übersehen
  def validateName(name: String)(using Raise[ValidationError]): String =
    if name.isBlank then Raise.raise(ValidationError.EmptyName("name"))
    else name.trim

  def validateAge(age: Int)(using Raise[ValidationError]): Int =
    if age < 0 then Raise.raise(ValidationError.NegativeAge(age))
    else age

  def validateCountry(country: String)(using Raise[ValidationError]): String =
    val allowed = Set("DE", "AT", "CH")
    if !allowed.contains(country) then Raise.raise(ValidationError.UnknownCountry(country))
    else country

  // Mehrere Effekte werden einfach kombiniert – direkter, lesbarer Code
  def buildUser(name: String, age: Int, country: String)(using Raise[ValidationError]): User =
    val validName    = validateName(name)
    val validAge     = validateAge(age)
    val validCountry = validateCountry(country)
    User(validName, validAge, validCountry)

  // ─── Fehler-Mapping zwischen Layern ───────────────────────────────────────

  enum ServiceError:
    case InvalidInput(msg: String)
    case Forbidden(msg: String)

  // Konvertierung von Validierungsfehlern in Service-Fehler ohne manuelles try/catch
  def createUser(name: String, age: Int, country: String)(using Raise[ServiceError]): User =
    given MapError[ValidationError, ServiceError] = MapError {
      case ValidationError.EmptyName(f)      => ServiceError.InvalidInput(s"Feld '$f' darf nicht leer sein")
      case ValidationError.NegativeAge(a)    => ServiceError.InvalidInput(s"Ungültiges Alter: $a")
      case ValidationError.UnknownCountry(c) => ServiceError.Forbidden(s"Land '$c' nicht erlaubt")
    }
    buildUser(name, age, country)

  // ─── Fehler-Akkumulation: alle Fehler auf einmal sammeln ──────────────────

  def validateUserAccumulating(name: String, age: Int)(using Raise[List[String]]): User =
    Raise.accumulate {
      val n = accumulating(if name.isBlank then Raise.raise("Name leer") else name)
      val a = accumulating(if age < 0 then Raise.raise("Alter negativ") else age)
      User(n, a, "DE")
    }

  // ─── Demo ──────────────────────────────────────────────────────────────────

  @main
  def runRaiseEffect(): Unit =
    println("=== Raise Effect Demo ===\n")

    // 1. Erfolgsfall
    val ok: Either[ValidationError, User] = Raise.either(buildUser("Alice", 30, "DE"))
    println(s"Erfolg:           $ok")

    // 2. Fehlerfall – kein Exception-Stacktrace, sondern typisierter Wert
    val err: Either[ValidationError, User] = Raise.either(buildUser("", 30, "DE"))
    println(s"Leerer Name:      $err")

    // 3. Union-Type statt Either
    val union: ValidationError | User = Raise.run(buildUser("Bob", -5, "DE"))
    println(s"Negatives Alter:  $union")

    // 4. Layer-Mapping
    val svc: Either[ServiceError, User] = Raise.either(createUser("", 30, "XX"))
    println(s"Service-Fehler:   $svc")

    // 5. Akkumulation – mehrere Fehler gesammelt, nicht nur der erste
    val acc: Either[List[String], User] = Raise.either(validateUserAccumulating("", -1))
    println(s"Alle Fehler:      $acc")
