# Multi-Agenten-System mit llm4s (Scala 3 / scala-cli)

Gleiche Aufgabe wie `../ai-sttpai-manual`: zu einer technischen Entscheidung wird ein ausgewogener Bericht erstellt -
diesmal mit [llm4s](https://github.com/llm4s/llm4s) (`llm4s-core` 0.4.1) statt `sttp-ai`.

## Agenten

| Agent               | Rolle                                | Läuft...              | Tool                                            |
|---------------------|--------------------------------------|-----------------------|-------------------------------------------------|
| **Fact-Researcher** | Fakten, Argumente, Quellen           | parallel zu Risk      | `duckduckgo_search` (llm4s built-in)            |
| **Risk-Analyst**    | Risiken, Kosten, Nachteile           | parallel zu Fact      | `calculate_tco` (eigenes Tool, `ToolBuilder`)   |
| **Synthesis**       | Fasst beide Berichte zusammen        | nach beiden Workern   | -                                               |

Der `Orchestrator` ist reiner Code (kein LLM-Call): Fan-out/Fan-in mit `ox.par`.

## Was llm4s hier übernimmt (vs. sttp-ai-manual)

| Thema                  | sttp-ai-manual                                  | llm4s                                                                  |
|------------------------|-------------------------------------------------|------------------------------------------------------------------------|
| Tool-Use-Loop          | selbst geschrieben (`AnthropicClient.chat`)     | `Agent.run(query, tools, ...)` iteriert und führt Tool-Handler aus    |
| Tool-Schema            | `ToolInputSchema` + circe-Codec                 | `Schema.object...` + `ToolBuilder(...).withHandler(...)` (upickle)    |
| Tool-Fehler            | Handler gibt JSON-Fehlerobjekt zurück           | Handler gibt `Left(msg)` zurück - wird dem Modell als Fehler gezeigt   |
| Web-Suche              | server-seitig (Anthropic `web_search`)          | client-seitig `DuckDuckGoSearchTool` (nur Kurzantworten/Definitionen)  |
| Provider-Konfiguration | `ClaudeConfig` im Code                          | `OpenRouterClient` direkt in `Llm.scala` (siehe Stolpersteine)         |
| Fehlerbehandlung       | Exceptions                                      | `Result[A] = Either[LLMError, A]`, im `for` verkettet                  |
| Max. Schritte          | -                                               | `maxSteps` (Standard hier: 10) begrenzt den Tool-Loop                   |

## Dateien

```
project.scala           scala-cli Direktiven (llm4s-core, ox)
logback.xml             Logging auf WARN (sonst flutet llm4s die Konsole mit DEBUG)
Llm.scala               LLMClient für den Requesty-Router
Agent.scala             Basisklasse: Rolle (System-Prompt) + Tools -> llm4s Agent
CalculateTcoTool.scala  Custom-Tool
AgentFactResearcher / AgentRiskAnalyst / AgentSynthesis.scala
Orchestrator.scala      ox.par Fan-out/Fan-in
Main.scala              schreibt output/*.md
```

## Ausführen

Voraussetzung: JDK 21+, Umgebungsvariable `ANTHROPIC_API_KEY` (Requesty-Key), z. B. `export ANTHROPIC_API_KEY=...`.

```bash
cd cli/ai-llm4s-agentic
scala-cli run . -- "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
```

Ergebnisse: `output/01_fact_researcher.md`, `02_risk_analyst.md`, `03_final_report.md`.

## Stolpersteine

- **Provider-Wahl in llm4s 0.4.1 (Requesty-Router, OpenAI-kompatibel):**
  - `openai`/`requesty`: nutzt das Azure-SDK, das den Pfad `/openai/deployments/<model>/chat/completions` baut -> **404**.
  - `anthropic`: verwirft Assistant-Nachrichten mit Tool-Calls und schickt Tool-Ergebnisse als Text
    (`[Tool result for ...]`) -> das Modell ruft das Tool **endlos erneut auf** ("Maximum step limit reached").
  - Lösung: `OpenRouterClient` direkt instanziieren (`Llm.scala`). Er postet an `${baseUrl}/chat/completions` im
    OpenAI-Format und funktioniert mit jedem OpenAI-kompatiblen Endpunkt. Über `LLMConnect` wäre er nur bei einer
    `baseUrl` mit `openrouter.ai` erreichbar.
- **Prompt vs. Tool-Aufrufe:** Bei Vergleichsfragen ("A oder B") ruft das Modell `calculate_tco` pro Option auf. Der
  Prompt erlaubt deshalb einen Aufruf je Option, `maxSteps` liegt bei 10.
- **API-Key:** bewusst simpel per `sys.env("ANTHROPIC_API_KEY")` gelesen (kein `.env`-Support, wirft `NoSuchElementException` wenn nicht gesetzt).
- **Versionen:** llm4s 0.4.1 (Scala 3.7.1, JDK 21) - API wird bis 1.0 noch umgebaut (`Agent.builder` auf `main` ist in
  0.4.1 noch nicht vorhanden; hier: `new Agent(client)`).
- **`duckduckgo_search`** nutzt die Instant-Answer-API: gut für Definitionen, keine echte Websuche.
- **Mix aus Tools:** llm4s führt alle Tools client-seitig aus, daher entfällt die Einschränkung "server-seitig vs.
  client-seitig" aus dem sttp-ai-Projekt.
