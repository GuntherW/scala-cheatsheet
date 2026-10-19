# Anwendung: Starten, Container, Traces ansehen

> Aufbau, Technik und Hintergründe: [Readme_Architecture.md](Readme_Architecture.md).

## 1. Überblick: Was brauche ich wofür?

| Ziel                            | Benötigte Container | Zusätzlich                         |
|---------------------------------|---------------------|------------------------------------|
| Anwendung nutzen (ingest / ask) | `postgres-rag`      | `ANTHROPIC_API_KEY` (Requesty-Key) |
| + Traces in OpenObserve ansehen | `openobserve`       | `OTEL_AUTHORIZATION` in `.env`     |

Ohne laufenden `openobserve`-Container funktioniert die Anwendung trotzdem; der Trace-Export schlägt dann still im
Hintergrund fehl (oder mit `RAG_OBS_OTEL=false` komplett abschalten).

## 2. Einmalige Vorbereitung

**Umgebungsvariablen** (Datei ``, nicht im Git; wird per direnv geladen - einmalig `direnv allow` im Ordner):

| Variable             | Zweck                                                         | Pflicht für |
|----------------------|---------------------------------------------------------------|-------------|
| `ANTHROPIC_API_KEY`  | Requesty-Key (Chat **und** Embeddings)                        | immer       |
| `OTEL_AUTHORIZATION` | `Basic <base64(email:passwort)>` für OpenObserve (OTLP-Login) | Traces      |

`OTEL_AUTHORIZATION` entspricht den Werten `OPENOBSERVE_ROOT_EMAIL` / `_PASSWORD` in `docker/.env.observability`:

```bash
cd docker && set -a && . ./.env.observability && set +a
printf 'OTEL_AUTHORIZATION="Basic %s"\n' "$(printf '%s:%s' "$OPENOBSERVE_ROOT_EMAIL" "$OPENOBSERVE_ROOT_PASSWORD" | base64 -w0)"
```

Ohne direnv: `set -a; . ./.env; set +a` vor dem Start.

**Docker-Secrets:** `docker/.env.observability` enthält Login und Passwort für OpenObserve (per `.gitignore`
ausgeschlossen, bewusst getrennt von
der versionierten `docker/.env`). Das Passwort muss stark sein (Groß-/Kleinbuchstaben, Ziffer, Sonderzeichen), sonst
startet der Container nicht.

Voraussetzungen: Docker + Docker Compose, scala-cli (lädt JDK 21 bei Bedarf selbst).

## 3. Container starten

Alle Befehle im Ordner `docker/`:

```bash
cd docker
ENVS="--env-file .env --env-file .env.observability"

# a) immer: Vektordatenbank (pgvector, Port 5434)
docker compose up -d postgres-rag

# b) optional: OpenObserve für die Traces (UI 5080, OTLP/gRPC 5081)
docker compose $ENVS --profile otel up -d openobserve
```

> **Wichtig:** Immer die Service-Namen angeben. `docker compose --profile ... up -d` **ohne** Namen startet zusätzlich
> alle
> Standard-Services (Kafka, Mongo, Postgres, ...).

Status prüfen / stoppen:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E "postgres-rag|openobserve"

docker compose $ENVS --profile otel stop openobserve
docker compose stop postgres-rag
```

Die Daten bleiben in Docker-Volumes erhalten (`docker_openobserve_data`, `dataRag`).

## 4. Anwendung starten

```bash
cd cli/ai-rag-llm4s

scala-cli run . -- generate-docs     # einmalig: 10 Beispiel-PDFs in docs/ erzeugen
scala-cli run . -- ingest            # PDFs -> Chunks -> Embeddings -> pgvector (löscht vorher die Tabelle, wiederholbar)
scala-cli run . -- ask "Wie viele Kilometer lang ist das Great Barrier Reef und wie viele Riffe umfasst es?"
scala-cli test .                     # Tests (ohne LLM/DB/Netz)
```

Weitere Beispielfragen: *"Wer hält den Marathon-Weltrekord der Frauen und wo wurde er aufgestellt?"* - und eine, die
**nicht** in den PDFs steht (*"Wie hoch ist der Mount Everest?"*): das Modell soll dann ehrlich antworten, dass es aus
den Auszügen nicht hervorgeht.

Beim Start meldet die Anwendung den Trace-Status: `[Observability] OpenTelemetry -> OpenObserve: an`.
Abschalten: `RAG_OBS_OTEL=false scala-cli run . -- ask "..."`.

## 5. Zugriff: Wo sehe ich was?

| Was               | Wo                                                       | Zugang                                                                                 |
|-------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------|
| OpenObserve (UI)  | http://localhost:5080                                    | E-Mail/Passwort: `OPENOBSERVE_ROOT_EMAIL` / `_PASSWORD` in `docker/.env.observability` |
| pgvector (Chunks) | `localhost:5434`, DB `ragdb`, Tabelle `llm4s_pdf_chunks` | User `postgres`, Passwort `PG_PASSWORD` aus `docker/.env`                              |

### 5.1 OpenObserve - Traces

1. http://localhost:5080 öffnen, anmelden.
2. Links **Traces** wählen, Organisation `default`, Stream `default`, Zeitraum z. B. *Last 1 hour*, **Run query**.
3. Pro Lauf erscheinen Spans des Services `ai-rag-llm4s`: `Embedding Usage - rag` (Embedding der Frage), `rag.retrieved`
   (Anzahl Chunks),
   `Token Usage - rag-answer` (Prompt-/Completion-Tokens) und `RAG answer` (Gesamtdauer). Sie sind **nicht verbunden**:
   jeder Span ist ein eigener
   Trace (llm4s 0.4.1 setzt keinen Parent, siehe Readme_Architecture.md). Die echte Dauer steht im Attribut
   `duration_ms`, nicht im Span selbst.
   Zeile anklicken für Details und `gen_ai_*`-Attribute (Modell, Tokens, Kosten).

Alternativ per API:

```bash
cd docker && set -a && . ./.env.observability && set +a
curl -s -u "$OPENOBSERVE_ROOT_EMAIL:$OPENOBSERVE_ROOT_PASSWORD" "localhost:5080/api/default/streams?type=traces"
```

Die Zähler in der Stream-Statistik hängen dem Eingang etwas hinterher; die Spans sind in der Suche früher sichtbar.

### 5.2 Gespeicherte Chunks in pgvector ansehen

```bash
docker exec postgres-rag psql -U postgres -d ragdb -c "select count(*) from llm4s_pdf_chunks"
docker exec postgres-rag psql -U postgres -d ragdb -c "select left(content, 80) from llm4s_pdf_chunks limit 5"
```

Spalten: `id`, `embedding`, `embedding_dim`, `content`, `metadata`, `created_at`.

## 6. Typische Fehler

| Symptom                                                        | Ursache / Lösung                                                                              |
|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `connection refused` zu `localhost:5434`                       | `postgres-rag` läuft nicht -> `docker compose up -d postgres-rag`                             |
| `Missing OpenAI API key` / `apiKey`                            | `ANTHROPIC_API_KEY` nicht gesetzt (`.env` laden / `direnv allow`)                             |
| OpenObserve bleibt leer                                        | `OTEL_AUTHORIZATION` fehlt/falsch, Container nicht gestartet oder Zeitraum in der UI zu klein |
| Log zeigt Verbindungsfehler zu `localhost:5081`                | `openobserve` läuft nicht -> starten (Abschnitt 3) oder `RAG_OBS_OTEL=false`                  |
| `openobserve` startet ständig neu                              | Root-Passwort zu schwach in `docker/.env.observability`                                       |
| `docker compose ... up -d` startet plötzlich Kafka, Mongo, ... | Service-Namen vergessen (siehe Abschnitt 3)                                                   |
| Warnung `variable is not set` beim Compose-Aufruf              | `--env-file .env --env-file .env.observability` fehlt                                         |

Weitere Hintergründe zu den Stolpersteinen: [Readme_Architecture.md](Readme_Architecture.md#stolpersteine-llm4s-041).
