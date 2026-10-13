# RAG-Lernbeispiel: PDFs, pgvector & llm4s (Scala 3.7.1 / scala-cli)

Gleiche Aufgabe wie [`ai-rag-langchain4j`](../ai-rag-langchain4j), nur mit **[llm4s](https://llm4s.org/)**
(`org.llm4s::llm4s-core:0.4.1`): 10 PDFs (`docs/`) werden gechunkt, embedded und in pgvector gespeichert;
eine Frage wird eingebettet, die ähnlichsten Chunks werden gesucht und zusammen mit der Frage an ein LLM
geschickt.

## Tech-Stack

| Zweck                     | Baustein                                                                                           |
|---------------------------|----------------------------------------------------------------------------------------------------|
| RAG-Orchestrierung        | `org.llm4s.rag.RAG` (Builder: `RAG.builder().withEmbeddings(...).withPgVector(...).build(...)`)    |
| PDF -> Text               | in `RAG.ingest(path)` eingebaut (`DefaultDocumentExtractor`)                                       |
| Chunking                  | `ChunkerFactory.Strategy.Sentence` (500 Zeichen, 50 Overlap)                                       |
| Embeddings                | `text-embedding-3-small` (1536 Dim.) über den Requesty-EU-Router, OpenAI-Embedding-Provider        |
| Vektordatenbank           | pgvector (`postgres-rag`, Port 5434) über `withPgVector(...)`, Tabelle `llm4s_pdf_chunks`          |
| LLM                       | `anthropic`-Provider gegen Requesty (`vertex/claude-sonnet-5-5@eu`)                                |
| Antwort                   | `rag.queryWithAnswer(frage)` - Retrieval + ein LLM-Call                                            |
| Konfiguration             | HOCON `resources/application.conf` + `Llm4sConfig`                                                 |
| Fehlerbehandlung          | `Result[A]` (Either) in for-Comprehensions, kein Exception-Wurf                                    |
| Tests                     | MUnit (Chunking, ohne LLM/DB)                                                                      |

## Vergleich mit langchain4j

| Schritt          | `ai-rag-langchain4j`                                | `ai-rag-llm4s`                                                      |
|------------------|-----------------------------------------------------|---------------------------------------------------------------------|
| Embeddings       | lokal, in-process ONNX (`all-MiniLM-L6-v2`, 384)    | API-basiert über Requesty (1536) - llm4s hat kein In-process-Modell |
| PDF-Parsing      | `ApachePdfBoxDocumentParser` + Loader               | in `RAG.ingest` enthalten                                           |
| Ingestion        | `EmbeddingStoreIngestor`                            | `rag.ingest(path)`                                                  |
| Retrieval + LLM  | `AiServices` (Proxy) + `EmbeddingStoreContentRetriever` | `rag.queryWithAnswer(...)`                                      |
| Tabelle leeren   | `dropTableFirst(true)`                              | `rag.clear()` vor `ingest`                                          |
| Fehler           | Exceptions                                          | `Either`/`Result`                                                   |
| Konfiguration    | Builder im Code, `sys.env`                          | HOCON-Datei                                                         |

## Projektstruktur

```
ai-rag-llm4s/
├── project.scala              # scala-cli Direktiven
├── resources/application.conf # Provider (Chat) + Embeddings -> Requesty
├── logback.xml
├── PgConfig.scala             # Postgres-Verbindung (sys.env mit Dev-Defaults)
├── RagFactory.scala           # baut LLM-Client + RAG aus Config
├── ingestion/                 # SampleDocs, GenerateSampleDocs, Ingestion
├── query/RagAssistant.scala   # Frage beantworten
├── Main.scala                 # generate-docs | ingest | ask
└── test/ChunkingTest.test.scala
```

## Ausführen

Voraussetzungen: `postgres-rag` läuft (`cd docker && docker compose up -d postgres-rag`), JDK 21 (wird von
scala-cli bei Bedarf geladen), `ANTHROPIC_API_KEY` (Requesty-Key) gesetzt - per `.env` + direnv (`direnv allow`)
oder `export`.

```bash
cd cli/ai-rag-llm4s
scala-cli run . -- generate-docs
scala-cli run . -- ingest
scala-cli run . -- ask "Wie viele Kilometer lang ist das Great Barrier Reef und wie viele Riffe umfasst es?"
scala-cli test .
```

## Stolpersteine (llm4s 0.4.1)

1. **Der `requesty`-Provider funktioniert nicht mit dem EU-Router:** In 0.4.1 nutzt er das Azure-OpenAI-SDK; bei
   einer eigenen `baseUrl` ruft das einen Azure-Pfad auf -> `404 page not found`. Lösung: `provider = "anthropic"` mit
   `baseUrl = "https://router.eu.requesty.ai"` (ohne `/v1`, das SDK hängt `/v1/messages` selbst an).
2. **Embeddings-`baseUrl` ohne `/v1`:** Der OpenAI-Embedding-Provider hängt `/v1/embeddings` selbst an.
3. **Embedding-Modellname:** Der Key ist nur für `azure/openai/text-embedding-3-small@francecentral` freigegeben
   (`GET /v1/models/embedding`), nicht für `openai/text-embedding-3-small`.
4. **Embedding-Key:** In 0.4.1 aus `llm4s.openai.apiKey` (hier an `ANTHROPIC_API_KEY` gebunden - Requesty-Key
   für alles).
5. **Embedding-Konfiguration an `RAG.build` übergeben:** `Llm4sConfig.embeddings()` liefert die Config, die als
   Resolver `_ => Right(cfg)` an `build` geht; sonst `Missing embedding provider config`.
6. **Eigene Tabelle:** `llm4s_pdf_chunks` (1536 Dim.), kollidiert nicht mit `document_chunks` (512) oder
   `langchain4j_pdf_chunks` (384).
7. **Neuere llm4s-Module** (`llm4s-rag`, `llm4s-openai`, ...) sind noch nicht veröffentlicht; 0.4.1 enthält alles
   in `llm4s-core`.
