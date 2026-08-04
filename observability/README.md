# Observability Demo

Drei Scala-Services demonstrieren Distributed Tracing, Metriken und Logs mit OpenTelemetry.

**Aufrufkette:** User → Service1 (Gateway, :8081) → Service2 (Processor, :8082) → Service3 (Calculator, :8083)

## Schnellstart

```bash
# 1. Backends starten
docker compose up -d

# 2. Services starten (je ein Terminal)
scala-cli run service3-calculator.scala
scala-cli run service2-processor.scala
scala-cli run service1-gateway.scala

# 3. Anfrage abschicken
curl -X POST http://localhost:8081/fibonacci \
     -H 'Content-Type: application/json' \
     -d '{"n": 10}'
```

**Grafana:** http://localhost:3000 (admin/admin)

---

## Die Backends: Was macht was?

### Tempo - Distributed Tracing

Tempo speichert **Traces**: die vollständige Aufrufkette einer Anfrage durch alle Services als Zeitstrahl (Flamegraph). Ein Trace besteht aus mehreren **Spans** - jeder Span ist eine einzelne Operation (z.B. "HTTP-Request empfangen", "Fibonacci berechnen").

Das Besondere: Mehrere Spans aus verschiedenen Services gehören zum selben Trace, weil sie dieselbe `traceId` teilen. Diese ID wird per `traceparent`-Header von Service zu Service weitergegeben (W3C-Standard).

**Früher: Jaeger**
Jaeger war lange der Standard für Distributed Tracing (entwickelt von Uber). Tempo ist der modernere Nachfolger:

| | Jaeger | Tempo |
|---|---|---|
| Speicher | Cassandra, Elasticsearch oder in-memory | Lokales Filesystem, S3, GCS, Azure |
| Ressourcen | Schwerer (eigene Index-Datenbank nötig) | Leichtgewichtig, kein Index erforderlich |
| Abfragesprache | Jaeger UI (einfach) | TraceQL (mächtig, ähnlich wie PromQL) |
| Grafana-Integration | Externe Datasource | Native, tiefe Integration |
| Betrieb | Mehrere Komponenten (Agent, Collector, Query) | Single Binary |

Tempo verzichtet bewusst auf einen Such-Index über Span-Inhalte - Traces werden nur per TraceID nachgeschlagen. Das macht es extrem ressourcenschonend. Für die Suche nach Traces (z.B. "alle Traces mit Fehler") wird Tempo mit Prometheus kombiniert (Metriken → TraceID → Trace).

---

### Loki - Log Aggregation

Loki sammelt und speichert **Logs** aller Services. Das Besondere: Loki indiziert **nur die Labels** (z.B. `service_name`, `host`), nicht den Inhalt der Log-Nachrichten. Das macht es um ein Vielfaches ressourcenschonender als klassische Log-Systeme.

**Früher: Elasticsearch + Kibana (ELK-Stack)**
Der ELK-Stack war jahrelang der De-facto-Standard für Log-Management. Loki ist die modernere, schmalere Alternative:

| | Elasticsearch + Kibana | Loki |
|---|---|---|
| Indizierung | Volltext-Index über alle Felder | Nur Labels (wie Prometheus) |
| Ressourcen | Sehr hoch (RAM, Disk, CPU) | Sehr gering |
| Abfragesprache | Elasticsearch DSL / KQL | LogQL (ähnlich wie PromQL) |
| Stärke | Komplexe Volltext-Suche, Aggregationen | Einfache Label-Filterung + Grep |
| Grafana-Integration | Plugin | Native |
| Betrieb | Komplex (Cluster, Shards, Replicas) | Einfach (Single Binary möglich) |

**Wann Elasticsearch besser ist:** Wenn man wirklich Volltext-Suche über Log-Inhalte braucht (z.B. "alle Logs die 'NullPointerException' enthalten, gruppiert nach Stack-Trace-Pattern"), ist Elasticsearch mächtiger. Loki ist kein Elasticsearch-Ersatz für komplexe Analysen - es ist bewusst einfacher gehalten.

**Wann Loki besser ist:** Wenn man Logs primär nach Service, Host oder Zeit filtert und die Inhalte dann per Regex durchsucht (was LogQL kann), ist Loki ausreichend und deutlich günstiger zu betreiben.

---

### Warum der Grafana Stack statt dem klassischen Setup?

Der klassische Stack (Jaeger + Prometheus + ELK) hat drei separate UIs: Jaeger UI für Traces, Grafana für Metriken, Kibana für Logs. Man musste zwischen Tabs wechseln und TraceIDs manuell kopieren.

Der Grafana Stack (Tempo + Prometheus + Loki) hat **eine einzige UI** für alle drei Signaltypen. Der entscheidende Mehrwert entsteht durch die **Korrelation**:

```
Metrik-Spike in Grafana sehen
  → In Tempo: Traces im gleichen Zeitfenster filtern
      → Langsamen Span identifizieren
          → "Open in Loki": Logs dieses Services zum exakten Zeitpunkt
```

Dieser Workflow funktioniert, weil Grafana Tempo, Loki und Prometheus als native Datasources kennt und die `traceId` als gemeinsamen Schlüssel zwischen den Signalen nutzt.

---

## Architektur

```
User
 │  POST /fibonacci
 ▼
Service1-Gateway (:8081)
 │  erstellt Root-Span, injiziert traceparent-Header
 │  POST /process
 ▼
Service2-Processor (:8082)
 │  liest traceparent-Header, erstellt Child-Span
 │  POST /fibonacci
 ▼
Service3-Calculator (:8083)
    berechnet Fibonacci, erstellt Child-Span

Alle drei Services senden Traces/Metriken/Logs
 ▼
OTel Collector (:4317/:4318)
 ├── Traces  → Tempo  (:3200)
 ├── Metriken → Prometheus (:9090)
 └── Logs    → Loki (:3100)
 ▼
Grafana (:3000)  ← einheitliche UI für alle Signale
```

---

## Konfigurationen

### otel-collector-config.yaml

Der OTel Collector ist das zentrale Herzstück: er empfängt alle Telemetriedaten der Services und leitet sie an die passenden Backends weiter.

```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318   # Services senden hierhin (OTLP/HTTP)
```
Die Services schreiben an Port **4318** (HTTP). Port 4317 wäre gRPC - beides ist OTLP, nur unterschiedliche Transportprotokolle.

```yaml
exporters:
  prometheus:
    endpoint: "0.0.0.0:8889"              # Prometheus scrapt diesen Port
    resource_to_telemetry_conversion:
      enabled: true                        # service.name wird zum Prometheus-Label
  otlp_grpc/tempo:
    endpoint: tempo:4317                   # Traces weiterleiten an Tempo
  otlp_http/loki:
    endpoint: http://loki:3100/otlp        # Logs an Loki (OTLP-Endpunkt seit Loki v3)
```
Jeder Signaltyp hat seinen eigenen Exporter. `resource_to_telemetry_conversion` sorgt dafür, dass `service.name` als Label in Prometheus landet - ohne diese Option sehen alle Services in Grafana identisch aus.

```yaml
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp_grpc/tempo, debug]   # debug: gibt Spans auf Konsole aus
    metrics:
      receivers: [otlp]
      exporters: [prometheus, debug]
    logs:
      receivers: [otlp]
      exporters: [otlp_http/loki, debug]
```
Pipelines verbinden Receiver → Processor → Exporter. `debug` gibt zusätzlich eine Zusammenfassung auf der Konsole des Collectors aus - nützlich zum Prüfen ob Daten ankommen.

---

### prometheus.yml

Prometheus arbeitet im Pull-Modell: er fragt den Collector regelmäßig ab.

```yaml
scrape_configs:
  - job_name: "otel-collector"
    static_configs:
      - targets: ["otel-collector:8889"]   # der prometheus-Exporter des Collectors
```
Der Collector stellt Metriken als Prometheus-Format unter Port 8889 bereit. Prometheus ruft diesen Endpunkt alle 15 Sekunden ab (`scrape_interval`). Die Metriken erscheinen dann mit dem Prefix `observability_` (konfiguriert im Collector).

---

### tempo.yaml

Tempo speichert Distributed Traces und macht sie über TraceQL abfragbar.

```yaml
distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317   # Collector sendet Traces hierhin
```
Tempo empfängt Spans vom Collector über OTLP/gRPC. Mehrere Spans mit derselben `traceId` werden zu einem Trace zusammengefasst - deshalb ist Context Propagation zwischen den Services entscheidend.

```yaml
storage:
  trace:
    backend: local     # für Produktion: s3, gcs oder azure
    wal:
      path: /var/tempo/wal   # Write-Ahead-Log: Spans werden erst hier gepuffert
```

---

### loki.yaml

Loki indiziert nur die Labels von Log-Einträgen, nicht den Inhalt - dadurch ist es deutlich ressourcenschonender als Elasticsearch.

```yaml
schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb        # Index-Format (TSDB = wie Prometheus)
      schema: v13        # aktuellstes Schema - effizientes Chunk-Format
```
Das Schema bestimmt wie Loki Logs intern speichert und indiziert. `v13` unterstützt strukturierte Metadaten direkt an Log-Einträgen.

```yaml
limits_config:
  allow_structured_metadata: true   # erlaubt Key-Value-Paare pro Log-Eintrag
```
Strukturierte Metadaten ermöglichen es, die `traceId` als durchsuchbares Feld am Log zu speichern - die Basis für den Sprung vom Log zum Trace in Grafana.

---

### grafana-datasources.yaml

Grafana wird mit vorkonfigurierten Datasources gestartet (auto-provisioning), kein manuelles Einrichten nötig.

```yaml
- name: Tempo
  jsonData:
    tracesToLogsV2:
      datasourceUid: loki
      filterByTraceID: true    # "Logs für diesen Trace" direkt in Tempo anklicken
    tracesToMetrics:
      datasourceUid: prometheus  # Metriken zum Zeitpunkt eines Traces anzeigen
    nodeGraph:
      enabled: true              # Service-Dependency-Graph automatisch zeichnen
```
Diese Verknüpfungen sind der Kern von Grafana als unified Observability-Plattform: man kann von einem Trace direkt zu den zugehörigen Logs springen und umgekehrt. Der `nodeGraph` zeichnet automatisch einen Graphen der Service-Abhängigkeiten aus den Trace-Daten.

```yaml
- name: Loki
  jsonData:
    derivedFields:
      - matcherRegex: '"traceId":"(\w+)"'
        name: TraceID
        url: "${__value.raw}"
        urlDisplayLabel: "Open in Tempo"
```
Loki sucht in jedem Log-Eintrag nach einer `traceId` per Regex. Wird eine gefunden, erscheint in Grafana ein klickbarer Link direkt zum Trace in Tempo.


```
User
 │  POST /fibonacci
 ▼
Service1-Gateway (:8081)
 │  erstellt Root-Span, injiziert traceparent-Header
 │  POST /process
 ▼
Service2-Processor (:8082)
 │  liest traceparent-Header, erstellt Child-Span
 │  POST /fibonacci
 ▼
Service3-Calculator (:8083)
    berechnet Fibonacci, erstellt Child-Span

Alle drei Services senden Traces/Metriken/Logs
 ▼
OTel Collector (:4317/:4318)
 ├── Traces  → Tempo  (:3200)
 ├── Metriken → Prometheus (:9090)
 └── Logs    → Loki (:3100)
 ▼
Grafana (:3000)  ← einheitliche UI für alle Signale
```

---

## Konfigurationen

### otel-collector-config.yaml

Der OTel Collector ist das zentrale Herzstück: er empfängt alle Telemetriedaten der Services und leitet sie an die passenden Backends weiter.

```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318   # Services senden hierhin (OTLP/HTTP)
```
Die Services schreiben an Port **4318** (HTTP). Port 4317 wäre gRPC - beides ist OTLP, nur unterschiedliche Transportprotokolle.

```yaml
exporters:
  prometheus:
    endpoint: "0.0.0.0:8889"    # Prometheus scrapt diesen Port
  otlp/tempo:
    endpoint: tempo:4317         # Traces weiterleiten an Tempo
  loki:
    endpoint: http://loki:3100/loki/api/v1/push  # Logs an Loki
```
Jeder Signaltyp hat seinen eigenen Exporter. Der Collector übersetzt das einheitliche OTLP-Format in das jeweils native Format des Backends.

```yaml
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/tempo, debug]   # debug: gibt Spans auf Konsole aus
    metrics:
      receivers: [otlp]
      exporters: [prometheus, debug]
    logs:
      receivers: [otlp]
      exporters: [loki, debug]
```
Pipelines verbinden Receiver → Processor → Exporter. `debug` gibt zusätzlich eine Zusammenfassung auf der Konsole des Collectors aus - nützlich zum Prüfen ob Daten ankommen.

---

### prometheus.yml

Prometheus arbeitet im Pull-Modell: er fragt den Collector regelmäßig ab.

```yaml
scrape_configs:
  - job_name: "otel-collector"
    static_configs:
      - targets: ["otel-collector:8889"]   # der prometheus-Exporter des Collectors
```
Der Collector stellt Metriken als Prometheus-Format unter Port 8889 bereit. Prometheus ruft diesen Endpunkt alle 15 Sekunden ab (`scrape_interval`). Die Metriken erscheinen dann mit dem Prefix `observability_` (konfiguriert im Collector).

---

### tempo.yaml

Tempo speichert Distributed Traces und macht sie über TraceQL abfragbar.

```yaml
distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317   # Collector sendet Traces hierhin
```
Tempo empfängt Spans vom Collector über OTLP/gRPC. Mehrere Spans mit derselben `traceId` werden zu einem Trace zusammengefasst - deshalb ist Context Propagation zwischen den Services entscheidend.

```yaml
storage:
  trace:
    backend: local     # für Produktion: s3, gcs oder azure
    wal:
      path: /tmp/tempo/wal   # Write-Ahead-Log: Spans werden erst hier gepuffert
```

---

### loki.yaml

Loki indiziert nur die Labels von Log-Einträgen, nicht den Inhalt - dadurch ist es deutlich ressourcenschonender als Elasticsearch.

```yaml
schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb        # Index-Format (TSDB = wie Prometheus)
      schema: v13        # aktuellstes Schema - effizienter Chunk-Format
```
Das Schema bestimmt wie Loki Logs intern speichert und indiziert. `v13` unterstützt strukturierte Metadaten direkt an Log-Einträgen.

```yaml
limits_config:
  allow_structured_metadata: true   # erlaubt Key-Value-Paare pro Log-Eintrag
```
Strukturierte Metadaten ermöglichen es, die `traceId` als durchsuchbares Feld am Log zu speichern - die Basis für den Sprung vom Log zum Trace in Grafana.

---

### grafana-datasources.yaml

Grafana wird mit vorkonfigurierten Datasources gestartet (auto-provisioning), kein manuelles Einrichten nötig.

```yaml
- name: Tempo
  jsonData:
    tracesToLogsV2:
      datasourceUid: loki
      filterByTraceID: true    # "Logs für diesen Trace" direkt in Tempo anklicken
    tracesToMetrics:
      datasourceUid: prometheus  # Metriken zum Zeitpunkt eines Traces anzeigen
    nodeGraph:
      enabled: true              # Service-Dependency-Graph automatisch zeichnen
```
Diese Verknüpfungen sind der Kern von Grafana als unified Observability-Plattform: man kann von einem Trace direkt zu den zugehörigen Logs springen und umgekehrt. Der `nodeGraph` zeichnet automatisch einen Graphen der Service-Abhängigkeiten aus den Trace-Daten.

```yaml
- name: Loki
  jsonData:
    derivedFields:
      - matcherRegex: '"traceId":"(\w+)"'
        name: TraceID
        url: "${__value.raw}"
        urlDisplayLabel: "Open in Tempo"
```
Loki sucht in jedem Log-Eintrag nach einer `traceId` per Regex. Wird eine gefunden, erscheint in Grafana ein klickbarer Link direkt zum Trace in Tempo.
