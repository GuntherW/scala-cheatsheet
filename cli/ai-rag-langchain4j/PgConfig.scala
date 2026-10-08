package rag

/** Verbindungsdaten zum `postgres-rag`-Container (siehe `docker/docker-compose.yml`, Service `postgres-rag`, pgvector-Image, Port 5434, Datenbank `ragdb`) - derselbe Container wie im
  * `ai-rag-sttpai`-Schwesterprojekt, hier aber mit einer eigenen Tabelle (`langchain4j_pdf_chunks`, siehe `VectorStore.scala`), damit sich beide Lernprojekte nicht in die Quere kommen.
  *
  * `langchain4j-pgvector` verbindet sich nicht über eine einzelne JDBC-URL, sondern über einzelne Felder (Host/Port/Datenbank/User/Passwort) - daher hier als separate Werte statt als
  * zusammengesetzter JDBC-String (vgl. `Db.scala` in `ai-rag-sttpai`).
  *
  * Liest direkt aus `sys.env` (echte Umgebungsvariablen) statt über eine eigene `.env`-Datei-Logik - für ein Lernbeispiel genügt das: Variablen vor dem Start setzen (`export PGRAG_PASSWORD=...`),
  * fertig. Alle fünf Werte haben zum lokalen `postgres-rag`-Container passende Defaults und müssen für den Standardfall also gar nicht gesetzt werden.
  */
object PgConfig:

  val host: String     = sys.env.getOrElse("PGRAG_HOST", "localhost")
  val port: Int        = sys.env.getOrElse("PGRAG_PORT", "5434").toInt
  val database: String = sys.env.getOrElse("PGRAG_DATABASE", "ragdb")
  val user: String     = sys.env.getOrElse("PGRAG_USER", "postgres")
  val password: String = sys.env.getOrElse("PGRAG_PASSWORD", "postgres")

  // Dieses Projekt nutzt 384-dimensionale AllMiniLmL6V2-Embeddings in dieser Tabelle
  val table: String = "langchain4j_pdf_chunks"
