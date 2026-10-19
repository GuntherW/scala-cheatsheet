package rag

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore

/** Erzeugt den pgvector-gestützten Embedding-Store für diese Pipeline. Im Unterschied zu `ai-rag-sttpai` (eigenes SQL-Init-Skript, eigene JDBC-Statements, siehe dortiges `VectorStore.scala`)
  * übernimmt `langchain4j-pgvector` hier Tabellenerzeugung, Index-Erstellung und das Einfügen/Suchen der Vektoren komplett - das ist der Kern des Unterschieds zwischen "RAG von Hand" und "RAG mit
  * fertiger Library".
  */
object VectorStore:

  /** @param dropTableFirst
    *   Vor jedem erneuten `ingest`-Lauf `true`, damit mehrfaches Einlesen keine Duplikate erzeugt (siehe `ingestion/Ingestion.scala`). Beim reinen Abfragen (`ask`, siehe `Main.scala`) `false`, damit
    *   die zuvor befüllte Tabelle erhalten bleibt.
    */
  def build(dropTableFirst: Boolean): PgVectorEmbeddingStore =
    PgVectorEmbeddingStore
      .builder()
      .host(PgConfig.host)
      .port(PgConfig.port)
      .database(PgConfig.database)
      .user(PgConfig.user)
      .password(PgConfig.password)
      .table(PgConfig.table)
      .dimension(EmbeddingModels.allMiniLm.dimension())
      .dropTableFirst(dropTableFirst)
      .build()
