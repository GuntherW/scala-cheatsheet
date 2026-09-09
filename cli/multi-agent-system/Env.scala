package agents

import io.github.cdimascio.dotenv.Dotenv

/** Lädt Umgebungsvariablen aus der `.env`-Datei im Projekt-Root (eine Ebene über diesem Ordner), analog zum Python-Pendant (`python-dotenv`).
  *
  * Nutzt die Standardbibliothek `dotenv-java`. Fällt automatisch auf echte Umgebungsvariablen zurück, falls die `.env`-Datei fehlt oder der Schlüssel dort nicht gesetzt ist.
  */
object Env:

  private val dotenv: Dotenv = Dotenv.configure()
    .directory(os.pwd.toString)
    .filename(".env")
    .ignoreIfMissing()
    .load()

  def get(key: String): Option[String] = Option(dotenv.get(key)).orElse(sys.env.get(key))

  def require(key: String): String = get(key)
    .getOrElse(throw new RuntimeException(s"Umgebungsvariable '$key' ist weder in .env noch im Environment gesetzt."))
