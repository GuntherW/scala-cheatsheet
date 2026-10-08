package rag

import rag.HashingTfIdfEmbedder.IdfModel

import java.sql.Connection

/** Zugriff auf die pgvector-gestützte "Vektordatenbank" (siehe `docker/sql-rag/01-ragdb.sql` für das Schema) - zwei Tabellen:
  *   - `document_chunks`: ein Chunk pro Zeile, inkl. seines Embedding-Vektors (`vector(512)`).
  *   - `tfidf_model`: genau eine Zeile mit den bei der Ingestion gefitteten IDF-Gewichten (siehe `HashingTfIdfEmbedder.fitIdf`) - wird beim Embedden der Nutzerfrage zur Query-Zeit erneut gebraucht.
  */
object VectorStore:

  /** Ein Treffer aus der Ähnlichkeitssuche (`search`). `distance` ist die Cosine-Distanz aus pgvector (`<=>`-Operator) - '''0 = identisch, 2 = exakt entgegengesetzt'''; je kleiner, desto ähnlicher.
    */
  final case class Hit(sourceFile: String, chunkIndex: Int, content: String, distance: Double)

  private def toVectorLiteral(embedding: Array[Float]): String =
    embedding.mkString("[", ",", "]")

  /** Leert beide Tabellen - vor jedem erneuten `ingest`-Lauf aufgerufen, damit mehrfaches Einlesen keine Duplikate erzeugt (siehe `Main.scala`). */
  def clearAll()(using conn: Connection): Unit =
    val st = conn.createStatement()
    try
      st.execute("TRUNCATE TABLE document_chunks")
      st.execute("DELETE FROM tfidf_model")
    finally st.close()

  def insertChunk(sourceFile: String, chunkIndex: Int, content: String, embedding: Array[Float])(using conn: Connection): Unit =
    val ps = conn.prepareStatement(
      "INSERT INTO document_chunks (source_file, chunk_index, content, embedding) VALUES (?, ?, ?, ?::vector)",
    )
    try
      ps.setString(1, sourceFile)
      ps.setInt(2, chunkIndex)
      ps.setString(3, content)
      ps.setString(4, toVectorLiteral(embedding))
      ps.executeUpdate()
    finally ps.close()

  def saveIdfModel(model: IdfModel)(using conn: Connection): Unit =
    val arrayLiteral = model.idf.mkString("{", ",", "}")
    val ps           = conn.prepareStatement(
      "INSERT INTO tfidf_model (id, dims, doc_count, idf) VALUES (1, ?, ?, ?::float8[])",
    )
    try
      ps.setInt(1, model.dims)
      ps.setInt(2, model.docCount)
      ps.setString(3, arrayLiteral)
      ps.executeUpdate()
    finally ps.close()

  def loadIdfModel()(using conn: Connection): Option[IdfModel] =
    val st = conn.createStatement()
    try
      val rs = st.executeQuery("SELECT dims, doc_count, idf FROM tfidf_model WHERE id = 1")
      try
        if rs.next() then
          val dims     = rs.getInt("dims")
          val docCount = rs.getInt("doc_count")
          val sqlArray = rs.getArray("idf").getArray.asInstanceOf[Array[Object]]
          val idf      = sqlArray.map(_.asInstanceOf[java.lang.Double].doubleValue)
          Some(IdfModel(dims, docCount, idf))
        else None
      finally rs.close()
    finally st.close()

  /** Cosine-Ähnlichkeitssuche über pgvector: `embedding <=> query` ist die eingebaute Cosine-Distanz, `ORDER BY ... LIMIT k` nutzt dafür (sofern vorhanden) den `ivfflat`-Index aus dem
    * `CREATE INDEX`-Statement im Init-Skript - das ist der Kern dessen, was eine "echte" Vektordatenbank gegenüber einer selbstgebauten Brute-Force-Suche in Scala leistet.
    */
  def search(queryEmbedding: Array[Float], topK: Int)(using conn: Connection): List[Hit] =
    val ps = conn.prepareStatement(
      "SELECT source_file, chunk_index, content, embedding <=> ?::vector AS distance " +
        "FROM document_chunks ORDER BY distance ASC LIMIT ?",
    )
    try
      ps.setString(1, toVectorLiteral(queryEmbedding))
      ps.setInt(2, topK)
      val rs = ps.executeQuery()
      try
        Iterator
          .continually(rs.next())
          .takeWhile(identity)
          .map(_ => Hit(rs.getString("source_file"), rs.getInt("chunk_index"), rs.getString("content"), rs.getDouble("distance")))
          .toList
      finally rs.close()
    finally ps.close()
