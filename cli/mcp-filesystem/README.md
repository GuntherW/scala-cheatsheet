# Filesystem MCP Server

Ein lokaler MCP-Server in Scala 3 (Scala CLI), der dem KI-Assistenten Zugriff auf das lokale Dateisystem gibt.

Gebaut mit [Chimp](https://github.com/softwaremill/chimp) (`chimp-server-ox`) und Ox (direct style).

---

## Was tut dieser Server?

LLMs haben von sich aus keinen Zugriff auf das lokale Dateisystem. Dieser MCP-Server stellt vier Tools bereit:

| Tool               | Beschreibung |
|--------------------|--------------|
| `list_directory`   | Listet den Inhalt eines Verzeichnisses auf (Dateien + Unterordner, mit Größen) |
| `read_file`        | Liest den Textinhalt einer Datei (max. 1 MB) |
| `search_in_files`  | Sucht per Regex rekursiv in Dateien eines Verzeichnisses, filterbar nach Dateiendung |
| `file_info`        | Gibt Metadaten zu einer Datei oder einem Verzeichnis aus (Größe, Datum, Rechte) |

---

## Server starten

```bash
scala-cli run cli/mcp-filesystem
```

Oder aus dem Ordner direkt:

```bash
scala-cli run .
```

Der Server läuft dann auf:

```
http://localhost:8181/mcp
```

---

## Technischer Aufbau

- **Transport:** HTTP via `OxServerHttpTransport` (direct style, kein ZIO)
- **Effect-System:** `Identity` (synchron, Ox-kompatibel)
- **Server-Framework:** Tapir + Netty (`NettySyncServer`)
- **Protokoll:** MCP über streamable HTTP (SSE-fähig für zukünftige Progress-Notifications)

Die Tools sind mit `StreamingMcpServer[Identity]().addTool(...)` registriert – das erlaubt später jederzeit die Erweiterung um Streaming-Tools mit Progress-Updates.

---

## Einbindung in OpenCode (lokal)

Damit OpenCode diesen MCP-Server als Tool nutzen kann, sind zwei Schritte nötig:

### 1. MCP-Server in `opencode.json` registrieren

OpenCode liest MCP-Server-Konfigurationen aus `~/.config/opencode/opencode.json` (global) oder aus einer `opencode.json` im Projektroot (projektlokal).

Folgendes eintragen:

```json
{
  "mcp": {
    "filesystem": {
      "command": "scala-cli",
      "args": ["run", "/absoluter/pfad/zu/cli/mcp-filesystem/FilesystemMcpServer.scala"],
      "type": "local"
    }
  }
}
```

> **Hinweis:** `type: "local"` bedeutet, OpenCode startet den Prozess selbst. Alternativ kann der Server vorab gestartet werden und als `type: "remote"` mit `url: "http://localhost:8181/mcp"` eingebunden werden.

Für den Remote-Fall (Server läuft bereits):

```json
{
  "mcp": {
    "filesystem": {
      "type": "remote",
      "url": "http://localhost:8181/mcp"
    }
  }
}
```

### 2. `AGENTS.md` anpassen (optional aber empfohlen)

Die Datei `AGENTS.md` im Projektroot (oder `~/.config/opencode/AGENTS.md` global) gibt dem Agenten Kontext darüber, welche Tools verfügbar sind und wie er sie nutzen soll.

Dort könnte ein Abschnitt ergänzt werden, z.B.:

```markdown
## MCP Tools

### filesystem (lokal)

Der MCP-Server `filesystem` gibt Zugriff auf das lokale Dateisystem. Nutze ihn, wenn du:
- Verzeichnisinhalte auflisten sollst: Tool `list_directory` mit `path`
- Dateien lesen sollst: Tool `read_file` mit `path`
- In Dateien suchen sollst: Tool `search_in_files` mit `directory`, `pattern`, optional `fileExtension` und `maxResults`
- Metadaten einer Datei brauchst: Tool `file_info` mit `path`

Alle Pfade müssen absolut sein oder relativ zum Arbeitsverzeichnis des Servers angegeben werden.
```

---

## Workflow: Remote vs. Local

| Modus    | Vorteil | Nachteil |
|----------|---------|----------|
| `local`  | OpenCode startet/stoppt den Prozess automatisch | Erster Aufruf langsam (Scala CLI kompiliert) |
| `remote` | Schnell, weil Server bereits warm läuft | Muss manuell gestartet werden |

**Empfehlung für Entwicklung:** Server vorab starten (`scala-cli run ...`), als `remote` einbinden – so entfällt die Compile-Latenz bei jedem OpenCode-Start.

---

## Wie der Agent die Tool-Schemas kennt

Der Agent muss die JSON-Struktur der Tools **nicht kennen** – er fragt den Server beim Start automatisch ab. Das ist Teil des MCP-Standards.

Beim Verbindungsaufbau ruft jeder MCP-Client `tools/list` auf. Der Server antwortet mit Name, Beschreibung und dem vollständigen **JSON-Schema** (Draft 2020-12) für jeden Tool-Input. Chimp generiert dieses Schema automatisch aus den Scala-Typen via `derives Schema` (Tapir).

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

> **Hinweis zu Default-Werten:** Tapir/Circe kennt keine Scala-Default-Werte zur Laufzeit – daher erscheinen `fileExtension` und `maxResults` als `required` im Schema, obwohl sie in der case class Defaults haben. Der Agent muss diese Felder also immer explizit mitschicken.

**Return-Werte** sind ebenfalls standardisiert: jedes Tool gibt ein `CallToolResult` zurück, das eine Liste von `content`-Objekten enthält (meist `{ "type": "text", "text": "..." }`), sowie ein `isError: Boolean`-Flag. Auch das ist Teil des MCP-Standards und dem Agenten beim Verbindungsaufbau bekannt.

---

## Erweiterungsideen

- **Progress-Reporting** bei langen Suchen (via `streamingServerLogic` + `ctx.reportProgress`)
- **`write_file`-Tool** zum Schreiben von Dateien
- **`move`/`copy`-Tools**
- **Glob-Pattern-Suche** (Dateien nach Muster finden, ohne Inhalt zu lesen)
- **`.gitignore`-Awareness** bei der Suche

---

## Beispiel-Prompts für OpenCode

Sobald der MCP-Server eingebunden ist, lösen folgende Aufträge den Einsatz der Tools aus:

- _„Was liegt alles im Ordner `~/projekte/scala/scala-cheatsheet/cli`?"_
- _„Lies die Datei `build.sbt` und erkläre mir die Projektstruktur."_
- _„Suche in `src/` nach allen Stellen, wo `Future` verwendet wird."_
- _„Wie groß ist die Datei `README.md` im Projektroot und wann wurde sie zuletzt geändert?"_
- _„Finde alle `.scala`-Dateien im Ordner `core/`, die das Wort `implicit` enthalten."_
