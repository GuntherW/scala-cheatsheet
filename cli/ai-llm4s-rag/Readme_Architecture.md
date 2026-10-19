# Architektur: RAG mit PDFs, pgvector & llm4s (Scala 3.7.1 / scala-cli)

> Dieses Dokument erklärt **wie** die Anwendung aufgebaut ist und **warum**. Zum Starten, zu den Containern und zum Ansehen von
> Traces/Metriken siehe [Readme_Usage.md](Readme_Usage.md).

10 PDFs (`docs/`) werden gechunkt, embedded und in pgvector gespeichert;
Eine Frage wird eingebettet, die ähnlichsten Chunks werden gesucht und zusammen mit der Frage an ein LLM
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
├── RagFactory.scala           # baut LLM-Client + RAG aus Config (hängt Observability ein)
├── ErrorReport.scala          # llm4s-Fehler nach Art einordnen (Rate-Limit, Timeout, Netzwerk, Auth, ...)
├── observability/             # OpenTelemetry-Tracing nach OpenObserve (siehe Abschnitt Observability)
├── ingestion/                 # SampleDocs, GenerateSampleDocs, Ingestion
├── query/RagAssistant.scala   # Frage beantworten
├── Main.scala                 # generate-docs | ingest | ask
├── test/                      # ChunkingTest, ObservabilityTest (ohne LLM/DB/Netz)
├── Readme_Architecture.md     # dieses Dokument
└── Readme_Usage.md            # Starten, Container, Traces/Metriken ansehen
```

## Observability

Traces gehen per **OpenTelemetry (OTLP/gRPC)** an **OpenObserve**. Der Code liegt in `observability/`:

```
  RAG (RAGConfig.withTracing) --> OtelSink --> OTLP/gRPC :5081 --> OpenObserve  http://localhost:5080
        ^
        +-- RagTracing: meldet nach der Antwort zusätzlich LLM-Tokens und Gesamtdauer nach
```

| Datei                              | Aufgabe                                                                                        |
|------------------------------------|------------------------------------------------------------------------------------------------|
| `observability/Observability.scala` | Verdrahtung: baut den Tracer aus der Config, `shutdown()` flusht den Batch-Exporter            |
| `observability/OtelSink.scala`     | `OpenTelemetryTracing` (llm4s) mit Endpoint, Basic-Auth-Header und Stream-Header für OpenObserve |
| `observability/RagTracing.scala`   | ergänzende Events, die `RAG` in llm4s 0.4.1 nicht selbst meldet                                |

Schalter: `rag.observability.enable.otel` (Env `RAG_OBS_OTEL`, Standard an; `false` = NoOp-Tracer). Start, Logins und Ansicht der
Traces: [Readme_Usage.md](Readme_Usage.md).

### Was llm4s 0.4.1 meldet - und was nicht

- `RAG` (mit `withTracing`) meldet **nur die Embedding-Nutzung**. Der LLM-Call der Antwort und die Gesamtdauer fehlen. Deshalb meldet
  `RagTracing` nach `queryWithAnswer` Retrieval (Anzahl Chunks), Token-Nutzung und RAG-Dauer manuell an denselben Tracer.
- Der OTel-Exporter von llm4s 0.4.1 startet für **jedes Event einen eigenen Span** ohne Parent und ohne gemeinsame `trace_id`: Ein `ask` ergibt
  in OpenObserve vier getrennte Traces mit Dauern von wenigen Millisekunden (die echte Dauer steht nur im Attribut `duration_ms`).
  Das ist eine Einschränkung von llm4s (auch in `main`), kein Konfigurationsfehler.
- Der Exporter exportiert **nur Traces** (`OtlpGrpcSpanExporter`), keine Metriken und keine Logs.
- **Beenden:** Der Exporter sendet gebündelt; ohne `Observability.shutdown()` (in `Main` per `finally`) gingen bei einem kurzen CLI-Lauf
  Spans verloren.

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
8. **OpenObserve-Passwort:** Der Root-Account verlangt ein starkes Passwort (Groß-/Kleinbuchstaben, Ziffer, Sonderzeichen), sonst crasht der
   Container in einer Restart-Schleife (Fehler steht nur tief im Log: `ZO_ROOT_USER_PASSWORD is too weak`).
9. **OpenObserve-OTLP:** gRPC-Port `5081` mit Basic-Auth im Header `authorization` sowie `organization` und `stream-name` - der OTel-Exporter von
   llm4s 0.4.1 (`OpenTelemetryConfig.headers`) reicht diese Header durch. Die Stream-Statistik in der API hängt dem Eingang etwas hinterher.
10. **Scala-Kommentare:** `/*` innerhalb eines Kommentars (z. B. ein Pfad mit `*.jsonl`) öffnet einen verschachtelten Kommentar -> `unclosed comment`.
