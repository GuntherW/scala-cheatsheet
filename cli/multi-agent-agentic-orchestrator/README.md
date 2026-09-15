# Agentischer, generischer Multi-Agenten-Orchestrator (Scala 3.9.0 / scala-cli)

Ziel: Verstehen, wie ein Multi-Agenten-System funktioniert -
als Vorbereitung auf die **CCAF (Claude Code Agent Framework)**
Zertifizierung/Prüfung.

Aufgabe des Systems: Zu einer technischen Entscheidung (z. B. "MongoDB vs.
PostgreSQL") wird ein ausgewogener, faktenbasierter Entscheidungsbericht
erstellt.

**Besonderheit dieser Version:** Der Orchestrator ist kein hartcodierter
Workflow mehr, sondern zweigeteilt in eine *agentische* Planning-Phase (ein
LLM - der `AgentOrchestrator` - entscheidet selbst, welche Worker-Agenten
in welcher Reihenfolge/Parallelität laufen) und eine *generische*
Execution-Phase (ein Dependency-Graph-Executor, der weder Anzahl noch
Identität der Agenten kennt). Siehe Abschnitt
["Vom Workflow zum Agenten"](#vom-workflow-zum-agenten) für die Details.

## Tech-Stack

| Zweck                                                             | Bibliothek                                                                                        |
|-------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Anthropic-/Claude-Client (Messages API, Tools, Structured Output) | [sttp-ai](https://sttp-ai.softwaremill.com/) (`claude`-Modul, `ClaudeSyncClient`)                 |
| JSON-Serialisierung/-Deserialisierung                             | [circe](https://circe.github.io/circe/) (bringt `sttp-ai` bereits mit, keine eigene Abhängigkeit) |
| JSON-Schema-Ableitung für Structured Output                       | [tapir](https://tapir.softwaremill.com/) `Schema` (bringt `sttp-ai` bereits mit)                  |
| Dateisystemzugriff (`output/`-Ordner)                             | [os-lib](https://github.com/com-lihaoyi/os-lib)                                                   |
| `.env`-Datei einlesen                                             | eigene, simple Implementierung (`Env.scala`, siehe unten)                                         |
| Nebenläufigkeit (paralleles Ausführen der Worker)                 | [ox](https://ox.softwaremill.com/) (`par`, strukturierte Nebenläufigkeit auf Virtual Threads)     |
| Tests (Plan-Validierung/Executor-Logik)                           | [MUnit](https://scalameta.org/munit/) (`scala-cli test .`)                                        |
| Build/Run ohne sbt-Projekt                                        | `scala-cli` mit `//> using` Direktiven                                                            |

Keine sbt-`build.sbt` nötig - alle Abhängigkeiten werden per Direktive in
`project.scala` deklariert; `scala-cli` löst sie automatisch über Coursier
auf.

**Voraussetzung:** JDK 21+ (wird von `ox` für Virtual Threads benötigt).

## Vom Workflow zum Agenten

Die Vorgänger-Version dieses Projekts hatte einen rein hartcodierten
Ablauf: `AgentOrchestrator.scala` rief explizit `par(AgentFactResearcher.research(topic), 
AgentRiskAnalyst.analyze(topic))` gefolgt von `AgentSynthesis.synthesize(...)` auf -
ein fixer Code-Pfad, der weder wusste noch entscheiden konnte, *ob* ein
Agent für das konkrete Thema überhaupt sinnvoll ist. Das ist ein **Workflow**: die Steuerungslogik ist vorprogrammiert,
das LLM wird nur für
die einzelnen Agenten-Aufrufe selbst genutzt.

Diese Version verschiebt genau eine Entscheidung - "welche Agenten in
welcher Reihenfolge/Parallelität?" - vom Scala-Code in ein LLM. Das macht
den Orchestrator selbst zu einem **Agenten** (im Sinne von "ein LLM trifft
eine Kontrollfluss-Entscheidung", siehe Anthropics
["Building Effective Agents"](https://www.anthropic.com/research/building-effective-agents)),
zusätzlich zu den bereits vorhandenen Worker-Agenten.

Gleichzeitig wurde der Orchestrator **generisch** gemacht: Er kennt weder
die Anzahl noch die Identität der Agenten. Ein neuer Agent wird
eingebunden, indem lediglich eine neue `AgentSpec` in `AgentRegistry.specs`
ergänzt wird - weder `Orchestrator` (Execution) noch `AgentOrchestrator`
(Planning) müssen dafür angepasst werden.

## Die Agenten & die generische Registry

| Agent                            | Rolle                                                             | `hardDependsOn`               | `isMandatory` | Tools                                   |
|----------------------------------|-------------------------------------------------------------------|-------------------------------|:-------------:|-----------------------------------------|
| **Fact-Researcher** (Worker)     | Sammelt Argumente, Fakten und Quellen *für* eine Technologie      | -                             |     nein      | `web_search` (server-seitig)            |
| **Risk-Analyst** (Worker)        | Sucht gezielt Fallstricke, Kosten, Sicherheitsbedenken, Nachteile | -                             |     nein      | `calculate_tco` (client-seitig, custom) |
| **Synthesis-Agent** (Aggregator) | Liest beide Outputs, löst Widersprüche auf, erstellt Endbericht   | fact-researcher, risk-analyst |    **ja**     | -                                       |

Jeder Agent registriert sich über eine `AgentSpec` (`AgentSpec.scala`) in
`AgentRegistry.specs` (`AgentRegistry.scala`) - einzig dort werden neue
Agenten eingetragen:

```scala
object AgentRegistry:
  def specs(topic: String): List[AgentSpec] = List(
    AgentFactResearcher.spec(topic),
    RiskAnalyst.spec(topic),
    AgentSynthesis.spec(topic),
  )
```

`hardDependsOn` ist eine harte Constraint, die der `AgentOrchestrator`
(LLM) beim Planen einhalten MUSS (vom `PlanValidator` notfalls
erzwungen/repariert); `isMandatory` erzwingt, dass ein Agent immer im Plan
enthalten ist, selbst wenn das LLM ihn vergisst (typischerweise ein
Aggregator, ohne den kein sinnvolles Endergebnis entsteht). Nicht als
Pflicht markierte Agenten (hier: Fact-Researcher, Risk-Analyst) darf der
`AgentOrchestrator` bewusst weglassen, wenn er sie für ein konkretes Thema
für irrelevant hält.

Ein Orchestrator koordiniert den Ablauf in zwei Phasen (Planning via LLM,
Execution generisch) - siehe unten.

## Ablaufdiagramm (Sequenz)

```mermaid
sequenceDiagram
    participant U as User
    participant O as Orchestrator (Executor)
    participant P as Orchestrator-Agent (Planner, LLM)
    participant F as Fact-Researcher
    participant R as Risk-Analyst
    participant S as Synthesis-Agent
    U ->> O: Thema (z.B. "MongoDB vs. PostgreSQL?")
    O ->> P: plan(topic, AgentRegistry.specs(topic))
    P -->> O: ExecutionPlan (Structured Output, 1 Request)
    Note over O: PlanValidator.validate(plan) - repariert/erzwingt hardDependsOn & isMandatory
    par Step 1 (laut validiertem Plan): ox.par
        O ->> F: execute(inputs = {})
        F ->> F: web_search (server-seitig, 1 Request genügt)
        F -->> O: Fakten & Quellen
    and
        O ->> R: execute(inputs = {})
        R ->> R: Turn 1: Modell fordert calculate_tco an (stop_reason=tool_use)
        R ->> R: Client führt calculate_tco lokal aus
        R ->> R: Turn 2: tool_result wird zurückgesendet, Modell antwortet final
        R -->> O: Risiken & Nachteile (inkl. TCO-Schätzung)
    end
    Note over O: Fan-in: par() kehrt erst zurück, wenn BEIDE fertig sind
    O ->> S: execute(inputs = {fact-researcher -> ..., risk-analyst -> ...})
    S -->> O: finaler Bericht
    O -->> U: finaler Bericht (outputsById(plan.finalAgentId)) + alle Zwischenergebnisse
```

## Architektur / Datenfluss

```mermaid
flowchart TD
    A[Main.scala] --> B[Orchestrator]
    B -->|1 . Planning| P[AgentOrchestrator]
    P -->|Structured Output| V[PlanValidator]
    V -->|validierter ExecutionPlan| B
    B -->|2 . Execution: Level-für-Level, ox . par pro Level| REG[AgentRegistry / AgentSpec]
    REG --> C[FactResearcher]
    REG --> D[RiskAnalyst]
    REG --> E[SynthesisAgent]
    C -->|nutzt Tool| C1[(web_search, server-seitig)]
    D -->|nutzt Tool| D1[(calculate_tco, client-seitig)]
    C -->|Ergebnis: Fakten| E
    D -->|Ergebnis: Risiken| E
    E -->|kein Tool, nur Kontext| F[Finaler Bericht]
    F --> G[output/*.md]

    subgraph Infrastruktur
        H[AnthropicClient] -. ClaudeSyncClient .-> I[(Anthropic API)]
        H -. circe + tapir .-> J[JSON De/Serialisierung + Structured-Output-Schema]
    end
    C -.-> H
    D -.-> H
    E -.-> H
    P -.-> H
```

## Client-seitiges Tool: der Tool-Use-Loop im Detail

Während `web_search` komplett vom Anthropic-Server ausgeführt wird (ein
einziger Request genügt), muss ein **client-seitiges (custom) Tool** wie
`calculate_tco` von uns selbst ausgeführt werden. Das erzeugt einen
Mehrschritt-Dialog ("Multi-Turn"). Beide Fälle - server-seitig und
client-seitig - werden von EINER einzigen Methode abgedeckt,
`AnthropicClient.chat`: Ein reiner `web_search`-Call terminiert die
Schleife bereits nach dem ersten Turn (`stopReason != "tool_use"`), ein
Custom-Tool wie `calculate_tco` löst hingegen den Multi-Turn-Loop aus:

```mermaid
sequenceDiagram
    participant R as RiskAnalyst (Agent)
    participant A as Anthropic API
    participant H as CalculateTcoTool.handler (lokale Scala-Funktion)
    R ->> A: MessageRequest(..., tools=[calculate_tco])
    A -->> R: stopReason="tool_use", content=[ToolUse: calculate_tco(technology, team_size)]
    R ->> H: handler(rawInput: Map[String, Json])
    H -->> R: Dummy-Ergebnis als JSON-String
    R ->> A: messages += [assistant: content(inkl. ToolUse), user: [ToolResult]]
    A -->> R: stopReason="end_turn", content=[Text: finaler Bericht]
```

Wichtige Punkte:

- Das Tool wird per **JSON-Schema** (`ToolInputSchema`/`PropertySchema` aus
  `sttp.ai.claude.models`) definiert - das Modell entscheidet selbst, *ob*
  und *mit welchen Parametern* es aufgerufen wird.
- `sttp-ai` bildet Anthropic's `content`-Feld bereits als typisiertes
  ADT (`sealed trait ContentBlock` mit `Text`, `ToolUse`, `ToolResult`, ...)
  ab - ein eigener `RawJson`-Wrapper wie in der Vorgänger-Implementierung
  entfällt dadurch vollständig.
- Die Original-Antwort des Modells (inkl. `ToolUse`-Block) muss als
  `assistant`-Nachricht unverändert in die Historie zurück, damit das
  Modell im nächsten Turn weiß, worauf sich das `ToolResult` bezieht (siehe
  aber Stolperstein zu `Thinking`-Blöcken unten).
- Das `ToolResult` wird über die `toolUseId` dem passenden Aufruf
  zugeordnet.
- Die Schleife (`AnthropicClient.chat`) läuft so lange, bis
  `stopReason != "tool_use"` ist.

**Hinweis zur Kombination von Tool-Typen:** Mischt man in einer Anfrage
server-seitige (`web_search`) und client-seitige Tools, erwartet dieser
Router-Endpunkt für **beide** Typen ein `tool_result` - `web_search`
verhält sich in dieser Kombination NICHT wie ein automatisch aufgelöstes
Server-Tool, sondern wie ein ganz normales `ContentBlock.ToolUse`, das wir
selbst beantworten müssten. Das können wir aber nicht, da uns keine eigene
Websuch-Implementierung zur Verfügung steht (das wurde per Smoke-Test
verifiziert: das Modell fordert `web_search` per `ToolUse` an, wir können
nur mit "kein Handler registriert" antworten - die Suche findet dann de
facto nicht statt). `AnthropicClient.chat` verbietet den Mix deshalb
bewusst per `require`. Deshalb nutzt der Risk-Analyst in diesem Beispiel
bewusst ausschließlich `calculate_tco`, um den Ablauf klar isoliert zu
zeigen.


## Planning-Phase: Der Orchestrator-Agent

Der `AgentOrchestrator` (`AgentOrchestrator.scala`) bekommt das Thema sowie
den Katalog aller `AgentSpec`s (id, Beschreibung, `hardDependsOn`,
`isMandatory`) als System-Prompt-Kontext und liefert einen `ExecutionPlan`
zurück:

```scala
case class ExecutionPlan(steps: List[List[String]], finalAgentId: String, reasoning: String)
```

Technisch genutzt wird Anthropics natives **Structured Output**
(`output_config`/`json_schema`), NICHT Tool-Use:

```scala
def chatStructured[T: Schema: Decoder](model: String, systemPrompt: String, userMessage: String): T =
  client.createMessageAs[T](MessageRequest.withSystem(model, systemPrompt, List(Message.user(userMessage)), maxTokens))
```

`sttp-ai` leitet das JSON-Schema automatisch aus der Case-Class `T` ab (via `tapir.Schema`, `derives Schema`) und parst
die Antwort direkt zu `T`
(`circe.Decoder`, `derives Decoder`). Der entscheidende Unterschied zu
Tool-Use (siehe unten, `AgentRiskAnalyst`/`calculate_tco`): Bei Tool-Use KANN
das Modell trotz Tool-Definition mit einem reinen Text-Turn antworten (`stopReason != "tool_use"`) - Structured Output
erzwingt dagegen auf
API-Ebene, dass die GESAMTE Antwort exakt dem Schema entspricht. Ein
Multi-Turn-Loop wie bei `chat` ist daher nicht nötig, ein
einzelner Request genügt (siehe `AnthropicClient.chatStructured`).

**Warum das trotzdem validiert werden muss:** Ein LLM-Aufruf ist nie
hundertprozentig verlässlich - selbst mit erzwungenem Schema kann der *Inhalt* des Plans falsch sein (unbekannte
agent-ids, verletzte
`hardDependsOn`-Abhängigkeiten, vergessene `isMandatory`-Agenten, ungültige
`finalAgentId`). Deshalb läuft jeder rohe Plan vor der Ausführung durch
`PlanValidator.validate` (`PlanValidator.scala`):

1. Unbekannte/doppelte agent-ids werden entfernt.
1. Fehlende Pflicht-Agenten (`isMandatory`) werden ergänzt.
1. Die verbleibende Reihenfolge dient nur noch als Tie-Breaker für einen
   stabilen topologischen Sort (Kahn-Algorithmus) nach `hardDependsOn` -
   das garantiert dependency-korrekte Level-Gruppierung, unabhängig davon,
   wie (in)korrekt das LLM ursprünglich gruppiert hatte. Ein Zyklus wird
   defensiv aufgebrochen statt den Executor zu blockieren.
1. `finalAgentId` wird validiert, sonst auf einen Pflicht-Agenten
   zurückgefallen.

`PlanValidator` ist reine, LLM-freie Logik und wird entsprechend mit MUnit
getestet (`PlanValidatorTest.test.scala`, `scala-cli test .`) - unabhängig
davon, wie zuverlässig das LLM tatsächlich antwortet.

## Execution-Phase: generischer Dependency-Graph-Executor

Der validierte Plan besteht aus Steps (`List[List[String]]`). Alle
agent-ids innerhalb eines Steps sind laut Plan unabhängig voneinander und
werden per `ox.par` parallel ausgeführt; der nächste Step startet erst,
wenn der aktuelle vollständig abgeschlossen ist:

```scala
var outputs = ListMap.empty[String, String]
for step <- plan.steps do
  val results = par(step.map(id => () => specs(id).execute(outputs)))
  outputs = outputs ++ step.zip(results)
```

`Orchestrator.scala` kennt an dieser Stelle weder die Anzahl noch die
Identität der Agenten - er iteriert ausschließlich über das, was
`AgentRegistry` bereitstellt und `AgentOrchestrator`/`PlanValidator`
geplant haben. Fact-Researcher und Risk-Analyst laufen (wie in der
Vorgänger-Version) weiterhin automatisch parallel, Synthesis automatisch
danach - aber ohne dass irgendwo im Code `par(FactResearcher..., RiskAnalyst...)`
hartcodiert steht.

## Warum parallel?

Fact-Researcher und Risk-Analyst sind voneinander **unabhängig**: keiner
braucht das Zwischenergebnis des anderen, um seine eigene Aufgabe zu lösen (kein gemeinsamer Eintrag in
`hardDependsOn`). Solche Worker lassen sich
parallelisieren (**Fan-out**), was die Gesamtlaufzeit deutlich reduziert -
denn die längste Wartezeit für ein Modell (Latenz) tritt nur einmal auf,
statt sich zu addieren. Erst der Synthesis-Agent braucht *beide* Ergebnisse
gleichzeitig (**Fan-in** / Synchronisationspunkt), läuft daher
zwangsläufig in einem eigenen, späteren Step.

Im Code (`Orchestrator.scala`) wird das mit [ox](https://ox.softwaremill.com/)
umgesetzt: `par(step.map(id => () => specs(id).execute(outputs)))` startet
alle Agenten EINES Steps auf eigenen Virtual Threads und kehrt erst
zurück, wenn ALLE fertig sind - Fan-out und Fan-in in einem einzigen
Aufruf, jetzt aber generisch für eine beliebige Anzahl von Agenten (`Seq[() => T]` statt der ursprünglichen festen
2er-Tupel-Variante von
`par`). Im Gegensatz zu `scala.concurrent.Future` handelt es sich dabei um **strukturierte Nebenläufigkeit**: Der Scope
von `par` garantiert, dass
keine "verwaisten" Hintergrund-Threads übrig bleiben, und schlägt eine der
beiden Berechnungen fehl, wird die andere automatisch abgebrochen (interrupted) und der Fehler propagiert - ganz ohne
manuelles
Error-Handling für beide Zweige einzeln.

## Wichtige Begriffe / Terminologie (für die CCAF-Prüfung)

- **Agent**: Eine Einheit, die einen LLM-Aufruf mit einer klar definierten
  Rolle (System-Prompt) kapselt und eine abgegrenzte Teilaufgabe löst. Hier
  als `abstract class Agent` in `Agent.scala` modelliert.
- **System Prompt**: Instruktion, die dem Modell *vor* der eigentlichen
  Nutzeranfrage mitgegeben wird und Rolle, Verhalten und Einschränkungen des
  Agenten festlegt (hier z. B. "Du bist der Fact-Researcher... nenne KEINE
  Risiken").
- **Multi-Agenten-System (Multi-Agent System, MAS)**: Mehrere spezialisierte
  Agenten arbeiten zusammen, um eine Aufgabe zu lösen, die ein einzelner
  Agent schlechter oder weniger strukturiert lösen würde (Separation of
  Concerns).
- **Worker Agent**: Ein Agent, der eine Teilaufgabe eigenständig bearbeitet,
  meist ohne Kenntnis der Zwischenergebnisse anderer Worker. Sorgt für
  unabhängige, unvoreingenommene Perspektiven (Fact-Researcher und
  Risk-Analyst kennen sich gegenseitig nicht).
- **Orchestrator**: In dieser Version zweigeteilt: Der **Orchestrator-Agent**
  (`AgentOrchestrator.scala`, LLM-Call) entscheidet *welche* Agenten *wann*
  (sequentiell oder parallel) aufgerufen werden sollen (Planning); der
  eigentliche `Orchestrator` (`Orchestrator.scala`) führt diesen Plan nur
  noch generisch aus (Execution) - er selbst ist reiner Code, kein
  LLM-Call. Diese Trennung ist der Kernunterschied zwischen einem
  hartcodierten **Workflow** (Vorgänger-Version) und einem **Agenten** als
  Kontrollfluss-Entscheider.
- **`AgentSpec` / `AgentRegistry`**: Generische, LLM-unabhängige
  Beschreibung eines Agenten (id, Beschreibung, `hardDependsOn`,
  `isMandatory`, `execute`-Funktion), zentral gesammelt in
  `AgentRegistry.specs`. Macht den Orchestrator generisch erweiterbar:
  neue Agenten werden nur hier ergänzt, ohne Executor oder Planner
  anzufassen.
- **`ExecutionPlan` / `PlanValidator`**: Der vom Orchestrator-Agent
  gelieferte Plan (Liste von parallel ausführbaren Steps + `finalAgentId`)
  ist LLM-generiert und daher nicht blind vertrauenswürdig.
  `PlanValidator.validate` erzwingt strukturelle Korrektheit (bekannte
  ids, eingehaltene `hardDependsOn`-Constraints via topologischem Sort,
  vorhandene Pflicht-Agenten, gültige `finalAgentId`) unabhängig von der
  Zuverlässigkeit der LLM-Antwort - und ist dadurch, im Gegensatz zum
  Rest des Systems, ohne echten Model-Call testbar (`scala-cli test .`).
- **Structured Output**: Anthropics natives Feature, die komplette
  Modell-Antwort auf ein vorgegebenes JSON-Schema zu erzwingen (`output_config`/`json_schema` in der Messages API). In
  `sttp-ai`
  abgebildet über `ClaudeSyncClient.createMessageAs[T]`
  (`AnthropicClient.chatStructured`) - das Schema wird automatisch aus
  einer Scala-Case-Class abgeleitet (`derives Schema` via tapir), die
  Antwort direkt zu dieser Case-Class geparst (`derives Decoder` via
  circe). Im Unterschied zu Tool-Use genügt dafür immer ein einzelner
  Request (kein `tool_use`/`tool_result`-Umweg), da das Modell gar nicht
  anders antworten kann als schemakonform. Genutzt vom
  `AgentOrchestrator` für den `ExecutionPlan`.
- **DAG (Directed Acyclic Graph) / Dependency-Graph-Executor**: Der
  generische `Orchestrator` interpretiert die `hardDependsOn`-Beziehungen
  der `AgentSpec`s als gerichteten, azyklischen Graphen und führt ihn
  Level-für-Level aus (`plan.steps`) - alle voneinander unabhängigen
  Agenten eines Levels parallel (`ox.par`), Level für Level sequentiell.
  Ersetzt das starre, hartcodierte Fan-out/Fan-in der Vorgänger-Version.
- **Synthesis- / Aggregator-Agent**: Ein Agent, der die Ausgaben mehrerer
  Worker als Kontext bekommt und daraus ein konsolidiertes Ergebnis
  erzeugt. Löst dabei auch inhaltliche Widersprüche zwischen den
  Worker-Ergebnissen auf.
- **Fan-out / Fan-in**: Verteilungsmuster, bei dem eine Aufgabe an mehrere
  unabhängige Worker verteilt wird (Fan-out) und deren Ergebnisse später an
  einer Stelle zusammengeführt werden (Fan-in). In Scala umgesetzt über
  `ox.par(...)`, das beide Berechnungen parallel startet und blockiert, bis
  beide Ergebnisse vorliegen.
- **Strukturierte Nebenläufigkeit (Structured Concurrency)**: Ein
  Nebenläufigkeits-Modell, bei dem der "Scope" einer parallelen Operation (hier: `par`) exakt an den Lebenszyklus des
  aufrufenden Codes gebunden
  ist - alle gestarteten Threads sind entweder erfolgreich beendet, oder
  wurden abgebrochen, BEVOR der umgebende Aufruf zurückkehrt. Verhindert
  "verwaiste" Hintergunds-Threads, wie sie bei freischwebenden `Future`s
  entstehen können. `ox` implementiert dies auf Basis von Java Virtual
  Threads (JDK 21+).
- **Tool Use / Function Calling**: Die Fähigkeit eines Modells, während der
  Antwortgenerierung definierte externe Werkzeuge aufzurufen, um an
  Informationen zu gelangen oder Aktionen auszuführen, die nicht im
  Trainingswissen enthalten sind (Grounding) bzw. nicht direkt im Modell
  ausgeführt werden können. Man unterscheidet:
- **Server-seitiges Tool**: Ein Tool wie `web_search_20250305`, das direkt
  vom Anthropic-Server ausgeführt wird - der Client muss den Tool-Aufruf
  nicht selbst abfangen und beantworten. Ein einzelner Request genügt (`AnthropicClient.chat`).
- **Client-seitiges (custom) Tool**: Ein selbst definiertes Tool (z. B.
  `calculate_tco` beim Risk-Analyst) mit eigenem JSON-Schema (`ToolInputSchema`). Das Modell liefert nur den *Wunsch*,
  das
  Tool
  aufzurufen (`stopReason == "tool_use"`), zurück - die eigentliche
  Ausführung übernimmt eine lokale Handler-Funktion (`CalculateTcoTool.handler`). Das Ergebnis muss danach explizit als
  `ContentBlock.ToolResult` an das Modell zurückgesendet werden (Multi-Turn-Dialog,
  `AnthropicClient.chat`).
- **Tool-Handler**: Die lokale Funktion, die ein client-seitiges Tool
  tatsächlich ausführt (hier: `CalculateTcoTool.handler` in
  `AgentRiskAnalyst.scala`). Bekommt die vom Modell gewählten Parameter als
  `Map[String, io.circe.Json]` und liefert einen String (meist JSON) als Ergebnis zurück.
- **`ToolUse` / `ToolResult` Block**: Content-Block-Typen im
  Anthropic-Message-Format (in `sttp-ai` als `ContentBlock.ToolUse` /
  `ContentBlock.ToolResult` modelliert). `ToolUse` = Aufrufwunsch des
  Modells (Name + Parameter + eindeutige `id`); `ToolResult` = die Antwort
  des Client darauf, referenziert über `toolUseId`.
- **Grounding**: Antworten eines Modells durch externe, verifizierbare
  Quellen (z. B. Websuche-Ergebnisse) absichern, statt sich nur auf
  internes Modellwissen zu verlassen.
- **Context Passing**: Das Weiterreichen der Ausgabe eines Agenten als
  Eingabe-Kontext für einen anderen Agenten (hier: Fakten + Risiken werden
  als Text in den Prompt des Synthesis-Agent eingebettet).
- **Content Block**: Die Antwort eines Anthropic-Modells besteht aus einer
  Liste von Content-Blöcken unterschiedlichen Typs (`Text`, `ServerToolUse`,
  `WebSearchToolResult`, ...), in `sttp-ai` als `sealed trait ContentBlock`
  mit Fallklassen abgebildet. Für den finalen Bericht werden nur die
  `Text`-Blöcke extrahiert (`AnthropicClient.chat`).
- **Codec (circe)**: Typklassen-basierte Serialisierungs-/
  Deserialisierungslogik für einen bestimmten Typ (`Codec[T]` bzw.
  `Codec.AsObject[T]`), von `sttp-ai` selbst für alle API-Modelle
  bereitgestellt bzw. per `derives Codec.AsObject` für eigene Typen (`CalculateTcoInput`/`CalculateTcoResult` in
  `AgentRiskAnalyst.scala`)
  ableitbar.
- **`ClaudeSyncClient` (sttp-ai)**: Der blockierende, hochsprachliche
  Claude-Client aus `sttp-ai`, der Requests direkt als Response-Werte
  zurückgibt und im Fehlerfall eine `ClaudeException`-Unterklasse wirft (statt `Either`, wie es der rohe `ClaudeClient`
  täte). Nutzt intern
  weiterhin `sttp-client4` als HTTP-Backend (`DefaultSyncBackend`, basiert
  auf `java.net.http.HttpClient`).
- **Virtual Thread**: Ein von der JVM (ab JDK 21) verwalteter, extrem
  leichtgewichtiger Thread. Im Gegensatz zu klassischen Plattform-Threads
  können davon Millionen gleichzeitig existieren, ohne dass jeder ein
  eigenes Betriebssystem-Thread belegt. `ox` nutzt Virtual Threads als
  Grundlage für `par`, `fork` & Co.

## Projektstruktur

```
research_scala/
├── project.scala          # scala-cli Direktiven: Scala-Version, Abhängigkeiten & Test-Framework (MUnit)
├── Env.scala               # Liest ANTHROPIC_API_KEY aus ../.env (via os-lib)
├── AnthropicClient.scala   # Wrapper um sttp-ai's ClaudeSyncClient + Logging + Tool-Use-Loop + Structured Output
├── Agent.scala             # Basisklasse Agent (kapselt Model-Call + Tools)
├── AgentSpec.scala         # Generische Agenten-Beschreibung (id, hardDependsOn, isMandatory, execute) für Registry/Planner
├── AgentRegistry.scala     # Zentrale Liste aller AgentSpecs - einziger Ort, um neue Agenten einzubinden
├── AgentFactResearcher.scala # Worker (web_search, server-seitig) + eigene AgentSpec
├── AgentRiskAnalyst.scala  # Worker (calculate_tco, client-seitiges Custom-Tool) + eigene AgentSpec
├── CalculateTcoTool.scala  # Definition & Ausführung des calculate_tco-Tools (Ein-/Ausgabe-Typen, JSON-Schema, Handler)
├── AgentSynthesis.scala    # Aggregator + eigene AgentSpec (hardDependsOn beide Worker, isMandatory=true)
├── ExecutionPlan.scala     # Case-Class für den Planungs-Output (Structured Output Schema)
├── AgentOrchestrator.scala # Planning-Phase (agentisch): LLM entscheidet den ExecutionPlan
├── PlanValidator.scala     # Validiert/repariert den Plan (reine Funktion, ohne LLM-Call)
├── PlanValidatorTest.test.scala # MUnit-Tests für PlanValidator (scala-cli test .)
├── Orchestrator.scala      # Execution-Phase (generisch): führt den validierten Plan Level-für-Level aus (ox.par)
├── Main.scala              # Einstiegspunkt (@main), schreibt output/*.md generisch via os-lib
├── output/                 # wird beim Ausführen erzeugt (Zwischen- & Endergebnisse)
└── README.md
```

## Ausführen

```bash
cd multi-agent-system_agentic_orchestrator
scala-cli run . -- "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
scala-cli test .   # PlanValidator-Tests (kein API-Call nötig)
```

Ohne Argument wird ein Standardthema verwendet. Die Ergebnisse landen
generisch für jeden vom Orchestrator-Agent tatsächlich geplanten Agenten
in `output/<Nummer>_<agent-id>.md` (z. B. `01_fact-researcher.md`,
`02_risk-analyst.md`, `03_synthesis.md`) sowie im finalen Bericht
`output/99_final_report.md`.

## Credentials

Der API-Key wird aus der `.env`-Datei im Projekt-Root (`.env` im
Projektordner, Variable `ANTHROPIC_API_KEY`, alternativ
`ANTHROPIC_AUTH_TOKEN`) geladen (`Env.scala`, eigene, simple
`os-lib`-basierte Implementierung ohne zusätzliche Dependency) - analog zum
Python-Pendant in `../tutorial/init.py` bzw. `../research/init.py`. Genutzt
wird der Requesty-Router (`https://router.eu.requesty.ai`) mit dem Modell
`vertex/claude-sonnet-5@eu`, konfiguriert über `ClaudeConfig(baseUrl = ...)`
aus `sttp-ai`. Authentifiziert wird - wie im offiziellen Anthropic-SDK -
über die Header `x-api-key` und `anthropic-version: 2023-06-01`, die
`sttp-ai` automatisch setzt.

## Stolpersteine mit sttp-ai

**1. Custom Base-URL statt offizieller Anthropic-Endpoint:** Dieses Projekt
spricht (wie das Python-Pendant) einen Requesty-Router statt
`https://api.anthropic.com` an. `sttp-ai`s `ClaudeConfig` unterstützt das
direkt über den `baseUrl`-Parameter (`Uri`) - `v1/messages` wird vom Client
selbst angehängt, man darf den Pfad also NICHT mit angeben (siehe
`AnthropicClient.scala`). `ClaudeConfig.fromEnv`/`ClaudeSyncClient.fromEnv`
wurden hier bewusst NICHT genutzt, da sie nur `sys.env` lesen - dieses
Projekt liest den API-Key stattdessen über die projekteigene
`Env.scala` (die zusätzlich `../.env` einliest und den Fallback-Namen
`ANTHROPIC_AUTH_TOKEN` unterstützt).

**2. `ContentBlock.Thinking` ohne `signature`-Feld:** Anthropic verlangt beim
Zurücksenden von `thinking`-Blöcken (z. B. nach einem `tool_use`-Turn)
eigentlich die unverändert erhaltene `signature`, um die Integrität der
Reasoning-Kette zu prüfen. `sttp-ai` 0.11.0 bildet `ContentBlock.Thinking`
jedoch nur mit einem einzigen Feld ab (`thinking: String`, keine
`signature`) - eine Signatur kann also gar nicht transportiert werden.
Sendet man einen (gelegentlich fast leeren) `thinking`-Block unverändert
zurück, lehnt die API den Request mit `"each thinking block must contain
thinking"` ab. Workaround in `AnthropicClient.chat`: leere
`Thinking`-Blöcke werden vor dem Zurücksenden herausgefiltert. Das behebt
das beobachtete Fehlerbild, ändert aber nichts daran, dass diese
sttp-ai-Version für Modelle mit **erzwungener** Signatur-Prüfung bei
nicht-leeren Thinking-Blöcken (echtes Extended Thinking) derzeit keine
korrekte Lösung anbietet - das wäre nur durch ein Upstream-Fix in
`sttp-ai` behebbar.

**3. Mischen von server- und client-seitigen Tools:** Unabhängig von der
Bibliothek gilt weiterhin: Mischt man in einer Anfrage server-seitige (`web_search`) und client-seitige Tools, verhält sich `web_search`
NICHT wie ein automatisch vom Server aufgelöstes Tool, sondern wie ein
ganz normales `ContentBlock.ToolUse` (Name `web_search`), das der Client
selbst per `ToolResult` beantworten müsste - das haben wir per Smoke-Test
verifiziert. Da uns keine eigene Websuch-Implementierung zur Verfügung
steht, verbietet `AnthropicClient.chat` diesen Mix bewusst per `require`
(statt still zu degradieren). Deshalb nutzt der Risk-Analyst in diesem
Beispiel bewusst ausschließlich `calculate_tco`.

**4. Eigene Tool-Eingabe-/Ergebnis-Typen bleiben nötig:** `sttp-ai` liefert
Tool-Eingabeparameter als rohes `Map[String, io.circe.Json]`
(`ContentBlock.ToolUse.input`). Für ein sauberes, typisiertes Case-Class-
Schema (hier `CalculateTcoInput`/`CalculateTcoResult`) genügt in Scala 3
weiterhin `derives ConfiguredCodec` (mit einem impliziten `Configuration`
für snake_case-JSON-Feldnamen bei camelCase-Scala-Feldern) - `circe` ist
als Abhängigkeit von `sttp-ai` bereits transitiv vorhanden, es muss keine
eigene JSON-Bibliothek mehr eingebunden werden.

**5. Structured Output (`createMessageAs`) funktioniert über den
Requesty-Router:** Vor der Umsetzung des `AgentOrchestrator` wurde per
Smoke-Test verifiziert, dass Anthropics natives `output_config`/
`json_schema`-Feature (`ClaudeSyncClient.createMessageAs[T]`) auch über
`router.eu.requesty.ai` funktioniert (nicht nur gegen die offizielle
`api.anthropic.com`) - keine Selbstverständlichkeit bei einem Proxy/Router,
der neuere API-Felder ggf. nicht durchreicht. Falls das in einer anderen
Umgebung/mit einem anderen Router nicht der Fall sein sollte: Ein
Fallback auf Tool-Use (analog `calculate_tco`, mit demselben
`ExecutionPlan`-Schema als `Tool.Custom`-Definition statt
`OutputFormat.JsonSchema`) wäre strukturell einfach nachrüstbar, siehe
`AnthropicClient.chat`.
