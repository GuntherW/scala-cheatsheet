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
scala-cli run FilesystemMcpServer.scala
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

## Erweiterungsideen

- **Progress-Reporting** bei langen Suchen (via `streamingServerLogic` + `ctx.reportProgress`)
- **`write_file`-Tool** zum Schreiben von Dateien
- **`move`/`copy`-Tools**
- **Glob-Pattern-Suche** (Dateien nach Muster finden, ohne Inhalt zu lesen)
- **`.gitignore`-Awareness** bei der Suche
