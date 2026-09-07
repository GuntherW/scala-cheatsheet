# Filesystem MCP Server

Ein lokaler MCP-Server (Model Context Protokoll) in Scala 3 (Scala CLI), der dem KI-Assistenten Zugriff auf das lokale
Dateisystem gibt.

Gebaut mit [Chimp](https://github.com/softwaremill/chimp) (`chimp-server-ox`) und Ox (direct style).

---

## Was tut dieser Server?

LLMs haben von sich aus keinen Zugriff auf das lokale Dateisystem. Dieser MCP-Server stellt folgende Tools bereit:

| Tool                | Beschreibung                                                                                         |
|---------------------|------------------------------------------------------------------------------------------------------|
| `list_directory`    | Listet den Inhalt eines Verzeichnisses auf (Dateien + Unterordner, mit Größen)                       |
| `read_file`         | Liest den Textinhalt einer Datei (max. 10 MB); optional mit `offset` und `limit` (Zeilennummern)    |
| `write_file`        | Erstellt oder überschreibt eine Datei; legt fehlende Elternverzeichnisse automatisch an              |
| `edit_file`         | Ersetzt einen eindeutigen String in einer Datei (`oldString` → `newString`)                          |
| `search_in_files`   | Sucht per Regex rekursiv in Dateien eines Verzeichnisses, filterbar nach Dateiendung                 |
| `glob`              | Findet Dateien per Glob-Pattern (z. B. `*.scala`, `**/*.ts`) ohne Inhalt zu lesen                   |
| `file_info`         | Gibt Metadaten zu einer Datei oder einem Verzeichnis aus (Größe, Datum, Rechte)                      |
| `create_directory`  | Legt ein Verzeichnis inkl. aller fehlenden Elternverzeichnisse an                                    |
| `move`              | Verschiebt oder benennt eine Datei oder ein Verzeichnis um                                           |
| `copy`              | Kopiert eine Datei oder ein Verzeichnis an einen neuen Ort                                           |

---

## Server starten

### HTTP-Modus (für manuelle Nutzung / Tests)

```bash
scala-cli run . --main-class filesystemMcpServer
```

Der Server läuft dann auf `http://localhost:8181/mcp`.

### stdio-Modus (für OpenCode)

```bash
scala-cli run . --main-class filesystemMcpServerStdio
```

Im stdio-Modus kommuniziert der Server über stdin/stdout – OpenCode startet ihn automatisch als Subprozess.

---

## Technischer Aufbau

Der Server hat zwei Einstiegspunkte:

| `@main`                    | Transport                          | Verwendung             |
|----------------------------|------------------------------------|------------------------|
| `filesystemMcpServer`      | HTTP via `OxServerHttpTransport`   | Manuell / Tests        |
| `filesystemMcpServerStdio` | stdio via `OxServerStdioTransport` | OpenCode / MCP-Clients |

Beide nutzen dieselbe `mcpServer`-Definition mit allen zehn Tools.

- **Effect-System:** `Identity` (synchron, Ox-kompatibel)
- **Server-Framework:** Tapir + Netty (`NettySyncServer`) für HTTP; JDK stdio für stdio
- **Protokoll:** MCP (JSON-RPC)

---

## Einbindung in OpenCode

### Empfehlung: stdio (`local`)

OpenCode startet den Server automatisch beim Start als Subprozess – kein manueller Start nötig. Die Kommunikation läuft
über stdin/stdout, kein Port wird belegt.

`opencode.jsonc` im Projektroot:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "filesystem": {
      "type": "local",
      "command": [
        "scala-cli", "run",
        "/home/gunther/projekte/scala/scala-cheatsheet/cli/mcp-filesystem",
        "--main-class", "filesystemMcpServerStdio"
      ],
      "enabled": true
    }
  },
  "experimental": {
    "mcp_timeout": 120000
  }
}
```

> **Wichtig:** `mcp_timeout` auf mindestens `120000` (2 Minuten) setzen.
> Beim ersten Start kompiliert Scala CLI den Server (~30–60 Sekunden).
> Danach startet er aus dem Cache in wenigen Sekunden.

> **Wichtig:** `command` muss ein Array sein, kein einzelner String.
> Den absoluten Pfad zu `cli/mcp-filesystem` angeben.

### Alternative: HTTP (`remote`)

Server einmal manuell starten, OpenCode verbindet sich per HTTP. Vorteil: kein Compile-Timeout beim OpenCode-Start.

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "filesystem": {
      "type": "remote",
      "url": "http://localhost:8181/mcp",
      "enabled": true
    }
  }
}
```

### Vergleich der Transportarten

| Modus           | Vorteil                                           | Nachteil                                    |
|-----------------|---------------------------------------------------|---------------------------------------------|
| `local` + stdio | Startet automatisch mit OpenCode, kein Port nötig | Erster Start langsam (Scala CLI kompiliert) |
| `remote` + HTTP | Schnell, Server bereits warm                      | Muss manuell gestartet werden               |

### `AGENTS.md` anpassen (optional aber empfohlen)

```markdown
## MCP Tools

### filesystem (lokal)

Der MCP-Server `filesystem` gibt Zugriff auf das lokale Dateisystem. Nutze ihn, wenn du:

- Verzeichnisinhalte auflisten sollst: Tool `list_directory` mit `path`
- Dateien lesen sollst: Tool `read_file` mit `path`
  - Für große Dateien: `offset` (1-basierte Zeilennummer) und `limit` (Anzahl Zeilen) angeben
- Dateien schreiben oder erstellen sollst: Tool `write_file` mit `path` und `content`
- Dateien gezielt bearbeiten sollst: Tool `edit_file` mit `path`, `oldString`, `newString`
  - Schlägt fehl, wenn `oldString` nicht eindeutig ist – dann mehr Kontext im `oldString` angeben
- In Dateien suchen sollst: Tool `search_in_files` mit `directory`, `pattern`, optional `fileExtension` und `maxResults`
- Dateien nach Namensmuster finden sollst: Tool `glob` mit `path` und `pattern` (z.B. `*.scala`, `**/*.ts`)
- Metadaten einer Datei brauchst: Tool `file_info` mit `path`
- Verzeichnisse anlegen sollst: Tool `create_directory` mit `path`
- Dateien oder Verzeichnisse verschieben/umbenennen sollst: Tool `move` mit `from` und `to`
- Dateien oder Verzeichnisse kopieren sollst: Tool `copy` mit `from` und `to`

Alle Pfade müssen absolut angegeben werden.
```

---

## Wie der Agent die Tool-Schemas kennt

Der Agent muss die JSON-Struktur der Tools **nicht kennen** – er fragt den Server beim Start automatisch ab. Das ist
Teil des MCP-Standards.

Beim Verbindungsaufbau ruft jeder MCP-Client `tools/list` auf. Der Server antwortet mit Name, Beschreibung und dem
vollständigen **JSON-Schema** (Draft 2020-12) für jeden Tool-Input. Chimp generiert dieses Schema automatisch aus den
Scala-Typen via `derives Schema` (Tapir).

Beispielantwort des laufenden Servers auf `tools/list`:

```json
{
  "tools": [
    {
      "name": "list_directory",
      "description": "Lists the contents of a directory on the local filesystem. Returns files and subdirectories with their types and sizes.",
      "inputSchema": {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "title": "ListDirInput",
        "type": "object",
        "required": ["path"],
        "properties": {
          "path": { "type": "string" }
        }
      }
    },
    {
      "name": "search_in_files",
      "description": "Searches for a regex pattern inside files of a directory (recursively).\nUse fileExtension to filter by extension (e.g. 'scala', 'md', '*' for all).\nReturns matching lines with file path and line number.",
      "inputSchema": {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "title": "SearchInFilesInput",
        "type": "object",
        "required": ["directory", "pattern", "fileExtension", "maxResults"],
        "properties": {
          "directory":     { "type": "string" },
          "pattern":       { "type": "string" },
          "fileExtension": { "type": "string" },
          "maxResults":    { "type": "integer", "format": "int32" }
        }
      }
    }
  ]
}
```

> **Hinweis zu Default-Werten:** Tapir/Circe kennt keine Scala-Default-Werte
> zur Laufzeit – daher erscheinen `fileExtension` und `maxResults` als
> `required` im Schema. Der Agent muss diese Felder immer explizit mitschicken.

**Return-Werte** sind ebenfalls standardisiert: jedes Tool gibt ein
`CallToolResult` zurück mit einer Liste von `content`-Objekten und einem
`isError: Boolean`-Flag. Strukturierte Tools liefern zusätzlich
`structuredContent` als JSON-Objekt.

---

## MCP Resources (minimales Beispiel)

Neben Tools kennt MCP noch **Resources** – Daten, die der Client (z. B. der Agent) gezielt per URI abrufen kann,
statt sie über einen Tool-Aufruf mit Parametern anzufordern. Chimp unterstützt zwei Varianten, beide sind hier
minimal beispielhaft eingebaut (`FilesystemMcpServer.scala`):

| API                | Zweck                                                        | Beispiel in diesem Projekt |
|---------------------|---------------------------------------------------------------|------------------------------|
| `resource(uri)`     | Feste, immer gleiche URI                                       | `readmeResource` → `file:///readme` liefert immer die README.md |
| `resourceTemplate(uriTemplate)` | URI mit `{variable}`-Platzhalter, matcht mehrere konkrete URIs | `fileResourceTemplate` → `file:///{filename}` liefert eine beliebige Datei im Projekt-Root |

**Wichtige Einschränkung:** Eine `{variable}` in Chimp-URI-Templates matcht immer nur **ein Pfadsegment** (kein `/`).
`file:///{filename}` matcht also `file:///README.md`, aber **nicht** `file:///some/sub/dir/README.md`. Für
verschachtelte Pfade bräuchte man mehrere Variablen (z. B. `file:///{dir}/{filename}`) oder eine eigene Lösung.

### Wie ich diese Resources jetzt nutzen kann

1. Server im HTTP-Modus starten: `scala-cli run . --main-class filesystemMcpServer`
2. Verfügbare Resources auflisten (JSON-RPC-Methode `resources/list` für feste Resources,
   `resources/templates/list` für Templates) – z. B. mit dem [MCP Inspector](#) oder direkt per `curl`:
   ```bash
   curl -s http://localhost:8181/mcp -H "Content-Type: application/json" \
     -H "Accept: application/json, text/event-stream" \
     -d '{"jsonrpc":"2.0","id":1,"method":"resources/list","params":{}}'
   ```
3. Eine Resource lesen (Methode `resources/read`, Parameter `uri`):
   ```bash
   curl -s http://localhost:8181/mcp -H "Content-Type: application/json" \
     -H "Accept: application/json, text/event-stream" \
     -d '{"jsonrpc":"2.0","id":2,"method":"resources/read","params":{"uri":"file:///readme"}}'
   ```
4. Über das Template eine andere Datei im Projekt-Root lesen, z. B. `.scalafmt.conf`:
   ```bash
   curl -s http://localhost:8181/mcp -H "Content-Type: application/json" \
     -H "Accept: application/json, text/event-stream" \
     -d '{"jsonrpc":"2.0","id":3,"method":"resources/read","params":{"uri":"file:///.scalafmt.conf"}}'
   ```

Ein MCP-fähiger Agent (z. B. opencode) ruft `resources/list` und `resources/templates/list` automatisch beim
Verbindungsaufbau ab und kann Resources dann genauso wie Tools referenzieren – der Unterschied ist nur, dass der
Agent hier gezielt eine URI anfragt, statt Tool-Parameter zu befüllen.

---

## MCP Prompts (minimales Beispiel)

Die dritte MCP-Primitive neben Tools und Resources sind **Prompts**: vordefinierte, parametrisierbare
Nachrichten-Vorlagen, die der Server anbietet und die der **Nutzer** (nicht das LLM selbst) gezielt auswählt – meist
über ein Slash-Command o. Ä. in der Host-UI (z. B. `/explain_file` in einem Chat-Client). Der Server liefert dann eine
fertige Liste von Chat-Nachrichten zurück, die an das LLM geschickt wird.

Chimp bietet dafür `prompt(name)` in `chimp.server`. Beispiel in diesem Projekt (`FilesystemMcpServer.scala`):

```scala
val explainFilePrompt = prompt("explain_file")
  .description("Erzeugt einen Prompt, der das LLM bittet, den Inhalt einer Datei zu erklären.")
  .argument("path", description = Some("Absoluter Pfad zur Datei"), required = true)
  .handle: args =>
    val path = args("path")
    GetPromptResult(
      messages = List(PromptMessage(role = Role.User, content = ToolContent.Text(text = s"Bitte lies die Datei $path und erkläre mir, was der Code darin tut."))),
      description = Some(s"Erklärungs-Prompt für $path")
    )
```

### Wie ich diesen Prompt jetzt nutzen kann

1. Server im HTTP-Modus starten: `scala-cli run . --main-class filesystemMcpServer`
2. Verfügbare Prompts auflisten (Methode `prompts/list`):
   ```bash
   curl -s http://localhost:8181/mcp -H "Content-Type: application/json" \
     -H "Accept: application/json, text/event-stream" \
     -d '{"jsonrpc":"2.0","id":1,"method":"prompts/list","params":{}}'
   ```
3. Prompt mit Argument abrufen (Methode `prompts/get`, `arguments` als Map):
   ```bash
   curl -s http://localhost:8181/mcp -H "Content-Type: application/json" \
     -H "Accept: application/json, text/event-stream" \
     -d '{"jsonrpc":"2.0","id":2,"method":"prompts/get","params":{"name":"explain_file","arguments":{"path":"/absoluter/pfad/zur/Datei.scala"}}}'
   ```
   Antwort ist eine fertige `messages`-Liste (hier eine einzelne User-Nachricht), die ein MCP-Client 1:1 als
   Chat-Prompt an das LLM weiterreichen kann.

In opencode (bzw. jedem MCP-fähigen Chat-Client) erscheinen registrierte Prompts meist als Slash-Commands
(`/explain_file`) mit einem Eingabefeld für die Argumente – der Nutzer wählt den Prompt aktiv aus, im Gegensatz zu
Tools, die das LLM selbstständig aufruft.

---

## Tools vs. Resources vs. Prompts – Faustregel

| Primitive   | Wer entscheidet über die Nutzung? | Typischer Zweck                          |
|-------------|-------------------------------------|-------------------------------------------|
| **Tool**    | Das LLM (Agent) selbst              | Aktionen/Berechnungen mit Parametern, ggf. Seiteneffekte |
| **Resource**| Nutzer / Host-Anwendung             | Daten anhängen, rein lesend, keine Logik |
| **Prompt**  | Nutzer (z. B. via Slash-Command)    | Vorgefertigte, parametrisierte Chat-Vorlage |

---

## Test ausführen

```bash
scala-cli test cli/mcp-filesystem
# oder aus dem Ordner:
scala-cli test .
```

Der Test startet einen eigenen Server-Instanz auf einem zufälligen Port – der produktive Server muss dafür nicht laufen.

### Testdateien im Überblick

| Datei                                        | Prüft |
|-----------------------------------------------|-------|
| `FilesystemMcpServerToolsTest.scala`          | Alle Tools, plus `tools/list`, `initialize`-Capabilities und Protokoll-Fehlerfälle |
| `FilesystemMcpServerResourcesTest.scala`      | `resource` und `resourceTemplate` |
| `FilesystemMcpServerPromptsTest.scala`        | `prompt` |
| `FilesystemMcpServerIntegrationTest.scala`    | Mehrschritt-Interaktion: Prompt abrufen → Pfad daraus mit einem Tool weiterverwenden |
| `FilesystemMcpServerStdioTest.scala`          | Server als echter Subprozess über stdio (der Pfad, den OpenCode nutzt) |
| `McpServerFixture.scala`                      | Testinfrastruktur: startet den Server per HTTP und liefert einen fertigen `McpClient` |

Alle Tests (außer dem stdio-Test) nutzen einen echten `chimp.client.McpClient`, der per HTTP mit einer
In-Process-Server-Instanz spricht — `client().callTool(...)` ist also kein Mock, sondern ein vollwertiger
MCP-Client-Aufruf.

---

## Erweiterungsideen

- **Progress-Reporting** bei langen Suchen (via `streamingServerLogic` + `ctx.reportProgress`)
- **`.gitignore`-Awareness** bei der Suche und beim Glob

---

## Beispiel-Prompts für OpenCode

Sobald der MCP-Server eingebunden ist, lösen folgende Aufträge den Einsatz der Tools aus:

- _„Was liegt alles im Ordner `~/projekte/scala/scala-cheatsheet/cli`?"_
- _„Lies die Datei `build.sbt` und erkläre mir die Projektstruktur."_
- _„Suche in `src/` nach allen Stellen, wo `Future` verwendet wird."_
- _„Wie groß ist die Datei `README.md` im Projektroot und wann wurde sie zuletzt geändert?"_
- _„Finde alle `.scala`-Dateien im Ordner `core/`, die das Wort `implicit` enthalten."_
- _„Schreibe eine neue Datei `Notes.md` mit folgendem Inhalt: …"_
- _„Ersetze in `Config.scala` den String `localhost` durch `example.com`."_
- _„Finde alle TypeScript-Dateien im Projekt."_
- _„Verschiebe `old/Util.scala` nach `new/Util.scala`."_
- _„Lege das Verzeichnis `src/test/resources` an."_
