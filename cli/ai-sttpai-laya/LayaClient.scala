package laya

import sttp.ai.jev.{JevConfig, JevModel, JevSyncClient}
import sttp.model.Uri

/** Konfiguriert den sttp-ai `JevSyncClient` so, dass er statt gegen die echte TypeSafe-API gegen einen lokal laufenden `laya-serve` spricht (siehe README.md). Der Client hängt den Pfad `v1/systemone`
  * selbst an `baseUrl` an - die `baseUrl` darf daher nur Host+Port enthalten, ohne Pfad.
  */
object LayaClient:
  private val baseUrl: Uri = Uri
    .parse(sys.env.getOrElse("LAYA_BASE_URL", "http://127.0.0.1:8000"))
    .fold(err => sys.error(s"Invalid LAYA_BASE_URL: $err"), identity)

  // laya-serve läuft ohne Auth, solange LAYA_API_KEY beim Start nicht gesetzt wurde;
  // die Jev-API verlangt trotzdem einen (beliebigen) Bearer-String.
  private val apiKey: String = sys.env.getOrElse("LAYA_API_KEY", "not-required-locally")

  // Modellname, den laya-serve kennt: "english", "multilingual", "typed-decisions"
  // (oder ein von laya-serve akzeptierter Alias).
  private val model: JevModel = JevModel.fromString(sys.env.getOrElse("LAYA_MODEL", "english"))

  /** Client mit dem Modell aus `LAYA_MODEL` (Default "english"). */
  def apply(): JevSyncClient = JevSyncClient(JevConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))

  /** Client mit explizit gewähltem Modell, unabhängig von `LAYA_MODEL` - z. B. "multilingual" oder "typed-decisions". */
  def apply(modelName: String): JevSyncClient =
    JevSyncClient(JevConfig(apiKey = apiKey, baseUrl = baseUrl, model = JevModel.fromString(modelName)))
