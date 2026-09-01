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

## Test ausführen

```bash
scala-cli test cli/mcp-filesystem
# oder aus dem Ordner:
scala-cli test .
```

Der Test startet einen eigenen Server-Instanz auf einem zufälligen Port – der produktive Server muss dafür nicht laufen.

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
