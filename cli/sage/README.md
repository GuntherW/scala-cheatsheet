# Sage – Beispielprojekt (Ox-Backend)

Beispielprojekt für die Scala-3-Bibliothek [Sage](https://ghostdogpr.github.io/sage) mit dem
[Ox](https://ox.softwaremill.com)-Backend gegen einen lokalen [Valkey](https://valkey.io)-Server.

Sage ist ein Redis-/Valkey-Client für Scala 3. `Main.scala` zeigt die wichtigsten Kommandos anhand kleiner,
eigenständiger Beispiele.

## Server starten

```bash
docker compose -f docker/docker-compose.yml up -d valkey
```

Der Service läuft auf Port `6379` (siehe `docker/docker-compose.yml`).

## Beispiel ausführen

```bash
scala-cli run cli/sage
```

## Was wird gezeigt?

| Abschnitt              | Kommandos                                                   |
|-------------------------|--------------------------------------------------------------|
| Strings                 | `set`, `get`, `incrBy`                                        |
| Hashes                  | `hSet`, `hGetAll`                                              |
| Lists                   | `rPush`, `lRange`                                              |
| Sets                    | `sAdd`, `sMembers`                                             |
| Sorted Sets             | `zAdd`, `zRangeWithScores`                                     |
| Keys & Expiry           | `exists`, `expire`, `ttl`                                      |
| Eigener Codec           | `ValueCodec.emap` für einen eigenen Typ (`User`)               |
| Pipeline                | `client.pipeline(...)` – mehrere Commands in einem Roundtrip   |
| Transaktion             | `client.transaction { tx => ... }` mit `watch`/`exec`          |
| Pub/Sub                 | `subscribeScoped` (liefert einen Ox-`Flow`), `publish`         |
| Streams                 | `xAdd`, `xLen`, `xRange`, `xGroupCreate`, `xReadGroup`, `xAck`  |
| Client-side caching     | `client.cached(...)`                                           |

## Aufbau

- `project.scala` – `//> using`-Direktiven (Scala-Version, Sage-Ox-Dependency)
- `Main.scala` – `@main def sageDemo()`, ein Abschnitt pro Kommandogruppe

Da Sage's Ox-Backend synchron im `Ox`-Scope arbeitet (kein `Future`/`ZIO`-Wrapping nötig), steht der komplette Code
in einem `ox.supervised { ... }`-Block; alle Hilfsfunktionen nehmen dafür `(using Ox)` entgegen.
