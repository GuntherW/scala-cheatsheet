package rag

import org.llm4s.error.*

/** Ordnet llm4s-Fehler nach Art ein - so lässt sich im Log sofort unterscheiden, ob man warten (Rate-Limit), retryen (Timeout/Netz) oder
  * die Konfiguration korrigieren muss (Authentifizierung).
  */
object ErrorReport:

  def describe(error: LLMError): String =
    val kind = error match
      case _: RateLimitError      => "RATE-LIMIT (später erneut versuchen)"
      case _: TimeoutError        => "TIMEOUT (Retry mit Backoff / Timeout erhöhen)"
      case _: NetworkError        => "NETZWERK (Endpoint erreichbar?)"
      case _: AuthenticationError => "AUTHENTIFIZIERUNG (API-Key prüfen)"
      case _: ServiceError        => "PROVIDER-FEHLER (Status/Modell prüfen)"
      case _                      => "SONSTIGER FEHLER"
    s"[$kind] ${error.formatted}"
