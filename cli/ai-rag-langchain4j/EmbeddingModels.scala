package rag

import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel

/** Das Embedding-Modell dieses Projekts: `all-MiniLM-L6-v2`, ein kleines Sentence-Transformer-Modell (384 Dimensionen), das langchain4j als ONNX-Modell mitliefert und komplett lokal/in-process
  * ausführt - im Gegensatz zu den API-basierten Embedding-Modellen (z. B. OpenAI) ist dafür '''kein eigener API-Key''' nötig. Für ein Lernprojekt ideal: Ingestion und Query-Embedding funktionieren
  * offline und kostenlos, nur die eigentliche Antwortgenerierung (siehe `LlmConfig.scala`) braucht einen LLM-Call.
  */
object EmbeddingModels:
  val allMiniLm: EmbeddingModel = new AllMiniLmL6V2EmbeddingModel()
