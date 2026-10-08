package rag

/** Eigene Embedding-Implementierung ohne externes ML-Modell: Hashing-Trick + klassisches TF-IDF.
  *
  * '''Warum Hashing statt klassischem TF-IDF mit explizitem Vokabular?''' Klassisches TF-IDF braucht ein Vokabular (Wort -> Spaltenindex), das mit der Anzahl unterschiedlicher Wörter im Korpus wächst -
  * und dieses Vokabular müsste zusätzlich zur Query-Zeit erneut vorliegen (gleiche Wort -> Index-Zuordnung wie bei der Ingestion). Der "Hashing-Trick" (siehe
  * [[https://en.wikipedia.org/wiki/Feature_hashing Feature Hashing]], u. a. aus `scikit-learn`s `HashingVectorizer` bekannt) hasht jedes Wort stattdessen direkt auf einen von `dims` festen Buckets
  * (`word.hashCode.abs % dims`) - das Ergebnis hat IMMER exakt `dims` Dimensionen, unabhängig vom tatsächlichen Vokabular. Das ist hier aus zwei Gründen wichtig:
  *   1. pgvector-Spalten (`vector(512)`, siehe `docker/sql-rag/01-ragdb.sql`) verlangen eine feste Dimension.
  *   1. Kein separates Vokabular muss persistiert/synchron gehalten werden - nur die IDF-Gewichte (siehe `fitIdf`), die ohnehin pro Bucket (nicht pro Wort) berechnet werden.
  *
  * Kollisionen (zwei verschiedene Wörter landen im selben Bucket) sind beim Hashing-Trick prinzipbedingt möglich und reduzieren die Trennschärfe geringfügig - bei `dims = 512` und den kleinen
  * Beispiel-Korpora dieses Lernprojekts in der Praxis vernachlässigbar.
  *
  * '''Ablauf (siehe `Ingestion.scala`):'''
  *   1. `fitIdf` einmalig über ALLE Chunks des Korpus aufrufen -> `IdfModel` (ein IDF-Gewicht pro Bucket + Gesamtanzahl Dokumente), wird in `tfidf_model` persistiert.
  *   1. `embed` pro Chunk (bei der Ingestion) und pro Nutzerfrage (beim Retrieval) mit DEMSELBEN `IdfModel` aufrufen - nur so landen beide Vektoren im selben Vektorraum und sind vergleichbar.
  */
object HashingTfIdfEmbedder:

  /** @param dims
    *   Anzahl Hash-Buckets = Dimension der erzeugten Vektoren (muss zur `vector(N)`-Spalte in Postgres passen).
    * @param docCount
    *   Anzahl Chunks, über die `idf` berechnet wurde - nur informativ/fürs Logging.
    * @param idf
    *   ein IDF-Gewicht pro Bucket, siehe `fitIdf`.
    */
  final case class IdfModel(dims: Int, docCount: Int, idf: Array[Double])

  private def tokenize(text: String): Array[String] =
    text.toLowerCase
      .split("[^\\p{L}\\p{N}]+")
      .filter(_.length > 1)

  private def bucketOf(token: String, dims: Int): Int =
    val h = token.hashCode
    math.floorMod(h, dims)

  /** Berechnet die inverse Dokumentfrequenz je Bucket über den gesamten Korpus (einmalig bei der Ingestion).
    *
    * `idf[bucket] = log((docCount + 1) / (df[bucket] + 1)) + 1` - die "+1"-Glättung (additive smoothing, analog zu `scikit-learn`s `TfidfTransformer(smooth_idf=True)`) verhindert Division durch 0
    * bzw. `log(0)` für Buckets, die in gar keinem Dokument vorkommen, und begrenzt den IDF-Wert für Buckets, die in JEDEM Dokument vorkommen, auf `log(1) + 1 = 1` statt `0` (ein Bucket, der in jedem
    * Chunk auftaucht, ist uninformativ, soll aber nicht komplett ausgeblendet werden).
    */
  def fitIdf(chunks: Seq[String], dims: Int = 512): IdfModel =
    val docFreq = Array.fill(dims)(0)
    for chunk <- chunks do
      val bucketsInThisChunk = tokenize(chunk).map(bucketOf(_, dims)).distinct
      for b <- bucketsInThisChunk do docFreq(b) += 1

    val n   = chunks.size
    val idf = docFreq.map(df => math.log((n + 1).toDouble / (df + 1).toDouble) + 1.0)
    IdfModel(dims, n, idf)

  /** Erzeugt den Embedding-Vektor eines Textes (Chunk oder Nutzerfrage) auf Basis eines zuvor gefitteten `IdfModel`.
    *
    * Schritte: Tokenisieren -> Rohhäufigkeit je Bucket zählen -> Normalisieren auf Termfrequenz (`count / Gesamtanzahl Wörter`) -> mit IDF-Gewicht des Buckets multiplizieren -> auf Länge 1
    * L2-normalisieren (macht die spätere Cosine-Similarity-Suche in pgvector numerisch robust und vergleichbar zwischen unterschiedlich langen Texten).
    */
  def embed(text: String, model: IdfModel): Array[Float] =
    val tokens = tokenize(text)
    if tokens.isEmpty then Array.fill(model.dims)(0f)
    else
      val counts     = Array.fill(model.dims)(0.0)
      for t <- tokens do counts(bucketOf(t, model.dims)) += 1.0
      val totalTerms = tokens.length.toDouble
      val tfIdf      = Array.tabulate(model.dims)(i => (counts(i) / totalTerms) * model.idf(i))

      val norm = math.sqrt(tfIdf.map(x => x * x).sum)
      if norm == 0.0 then tfIdf.map(_.toFloat)
      else tfIdf.map(x => (x / norm).toFloat)
