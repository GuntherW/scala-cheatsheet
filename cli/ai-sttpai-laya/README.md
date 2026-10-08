# ai-sttpai-laya

Minimales `scala-cli`-Projekt: Nutzt sttp-ai's `jev`-Modul (Scala-3-Client
für TypeSafes Jev-Protokoll, `POST /v1/systemone`) gegen einen **lokal
laufenden `laya-serve`** (Python-Paket [Laya](https://github.com/NandhaKishorM/laya))
statt gegen die echte TypeSafe-Cloud-API. Keine Kosten, keine Internet-
Abhängigkeit zur Laufzeit (außer dem einmaligen Checkpoint-Download).

Hintergrund/Kontext (Systemvoraussetzungen, Alternativen wie openjev/SemIf):
siehe [`docs/ai/jev.md`](../../docs/ai/jev.md) im Projekt-Root.

## Voraussetzungen

- JDK 17+ und [`scala-cli`](https://scala-cli.virtuslab.org/)
- [`uv`](https://docs.astral.sh/uv/) (holt bei Bedarf selbst ein passendes Python 3.10+)

## 1. Laya lokal installieren (mit uv)

```bash
mkdir -p ~/bin/laya && pushd ~/bin/laya

# uv-Projekt anlegen; Python 3.12 explizit, da PyTorch neue Python-Versionen oft verzögert unterstützt.
# --name ist nötig: Der Projektname darf nicht "laya" sein (= Verzeichnisname), sonst
# scheitert "uv add" mit "self-dependencies are not permitted".
uv init --bare --name laya-local --python 3.12 .

# "serve"-Extra installiert zusätzlich FastAPI/uvicorn für laya-serve
uv add "laya[serve]"

# Installation prüfen (lädt noch kein Modell herunter)
uv run python -I -c "import laya; print(laya.__version__)"
popd
```

Kein manuelles `venv`/`pip` nötig: `uv` legt `.venv` und `uv.lock` selbst an.

### 1 a Laya lokal updaten

```bash
pushd ~/bin/laya
uv lock --upgrade-package laya && uv sync
popd
```

## 2. `laya-serve` starten

`laya-serve` implementiert den Jev-kompatiblen Endpunkt `POST /v1/systemone`
sowie `GET /health`.

Im Vordergrund (blockiert das Terminal bis `Ctrl+C`):

```bash
pushd ~/bin/laya
LAYA_HOST=127.0.0.1 LAYA_PORT=8000 LAYA_MODELS=english uv run laya-serve
popd
```

Im Hintergrund (Terminal bleibt frei, Log in `laya-serve.log`):

```bash
cd ~/bin/laya
LAYA_HOST=127.0.0.1 LAYA_PORT=8000 LAYA_MODELS=english \
  nohup uv run laya-serve > laya-serve.log 2>&1 &
```

Beim ersten Request (bzw. beim Start, falls `LAYA_PRELOAD=1`) wird der
Checkpoint des gewählten Modells von Hugging Face heruntergeladen - das
kann abhängig von Verbindung und Modellgröße einige Sekunden bis Minuten
dauern. Danach läuft alles aus dem lokalen Cache.

Healthcheck:

```bash
curl -s localhost:8000/health
```

### Wichtige Umgebungsvariablen von `laya-serve`

| Variable          | Bedeutung                                                                                                    | Default                      |
|-------------------|--------------------------------------------------------------------------------------------------------------|------------------------------|
| `LAYA_HOST`       | Bind-Adresse                                                                                                 | `0.0.0.0`                    |
| `LAYA_PORT`       | Bind-Port                                                                                                    | `8000`                       |
| `LAYA_DEVICE`     | Torch-Device für jeden Checkpoint (`cpu`, `cuda`, ...)                                                       | auto                         |
| `LAYA_PRELOAD`    | Checkpoints beim Start laden statt lazy beim ersten Request                                                  | `1`                          |
| `LAYA_MODELS`     | Kommagetrennte Liste **vorab** zu ladender Modelle (`english`,`multilingual`,`typed-decisions`); leer = alle | alle                         |
| `LAYA_MAX_LOADED` | Anzahl Checkpoints, die gleichzeitig resident im Speicher bleiben                                            | `2`                          |
| `LAYA_API_KEY`    | Bearer-Token, das Clients mitschicken müssen                                                                 | nicht gesetzt (= keine Auth) |

**Wichtig: `LAYA_MODELS` ist keine Zugriffsbeschränkung.** Es legt nur fest,
welche Checkpoints beim Start vorab geladen werden (Preload). Ein Request
mit einem anderen Modell (z. B. `model=multilingual`, während nur mit
`LAYA_MODELS=english` gestartet wurde) wird trotzdem bedient - der
Checkpoint wird dann beim ersten Request dieses Modells einfach **lazy
nachgeladen**. Das erklärt z. B., warum `/health` nach dem Ausführen von
`laya.mainMultilingual`/`laya.mainTypedDecisions` plötzlich mehr geladene
Modelle zeigt, als beim Start von `laya-serve` angegeben wurden.

`LAYA_MAX_LOADED` (Default `2`) begrenzt, wie viele Checkpoints
gleichzeitig resident bleiben. Nutzt du - wie die drei Beispiele in diesem
Projekt - alle drei Modelle, wird bei jedem Wechsel zum dritten Modell ein
anderer Checkpoint wieder verdrängt (LRU) und beim nächsten Request neu
geladen (spürbar langsamer). Um alle drei dauerhaft resident zu halten:

```bash
pushd ~/bin/laya
LAYA_HOST=127.0.0.1 LAYA_PORT=8000 \
  LAYA_MODELS=english,multilingual,typed-decisions LAYA_MAX_LOADED=3 \
  uv run laya-serve
popd
```

**Sicherheitshinweis:** Ohne `LAYA_API_KEY` ist der Server komplett
unauthentifiziert. Lokal auf `127.0.0.1` ist das für einen Testlauf okay;
sobald `LAYA_HOST=0.0.0.0` (Default!) gesetzt und der Rechner im Netz
erreichbar ist, unbedingt `LAYA_API_KEY` setzen (siehe Laya `SECURITY.md`).

### `laya-serve` wieder stoppen

Im Vordergrund: `Ctrl+C` im selben Terminal (uvicorn fährt sauber herunter,
`INFO: Shutting down` in der Ausgabe).

Von überall, mit einem einzigen Befehl:

```bash
pkill -f '[l]aya-serve'     # SIGTERM, sauberes Herunterfahren
```

Die eckigen Klammern verhindern, dass `pkill` die eigene Shell trifft (deren
Kommandozeile sonst ebenfalls `laya-serve` enthielte). Prüfen:

```bash
curl -sf localhost:8000/health || echo "gestoppt"
```

Alternativ gezielt über den Port (nützlich bei mehreren Instanzen):

```bash
kill $(lsof -t -iTCP:8000 -sTCP:LISTEN)
```

Notfalls hart beenden: `pkill -9 -f '[l]aya-serve'`.

## 3. Dieses Scala-Projekt ausführen

Das Projekt enthält drei Beispiele, je eines pro Laya-Checkpoint. Da
mehrere `@main`-Methoden im selben Verzeichnis liegen, muss die gewünschte
explizit über `--main-class` ausgewählt werden:

```bash
cd cli/ai-sttpai-laya

# "english"-Checkpoint (Main.scala)
scala-cli run . --main-class laya.main

# "multilingual"-Checkpoint, deutschsprachiges Ticket (MainMultilingual.scala)
scala-cli run . --main-class laya.mainMultilingual

# "typed-decisions"-Checkpoint, auf Entscheidungs-Workflows feingetunt (MainTypedDecisions.scala)
scala-cli run . --main-class laya.mainTypedDecisions
```

Damit alle drei funktionieren, muss `laya-serve` mit allen drei Modellen
gestartet werden, z. B. `LAYA_MODELS=english,multilingual,typed-decisions`
(oder `LAYA_MODELS` ganz weglassen, dann lädt `laya-serve` alle).

Erwartete Ausgabe von `laya.main` (Werte können je nach Modell leicht
variieren):

```
Department: billing (confidence 0.9266)
Urgency score: 1.5912 -> 2
Churn risk probability: 0.878
Model used: laya-rl-agent, requestId: None
```

## 4. Konfiguration dieses Projekts (Umgebungsvariablen)

| Variable        | Bedeutung                                                           | Default                 |
|-----------------|---------------------------------------------------------------------|-------------------------|
| `LAYA_BASE_URL` | Basis-URL des lokalen `laya-serve` (ohne Pfad!)                     | `http://127.0.0.1:8000` |
| `LAYA_API_KEY`  | Bearer-Token, falls `laya-serve` mit `LAYA_API_KEY` gestartet wurde | `not-required-locally`  |
| `LAYA_MODEL`    | Modellname/-alias, der im Request als `model` gesendet wird         | `english`               |

Siehe `LayaClient.scala`.

## Troubleshooting

- **`Connection refused`**: `laya-serve` läuft nicht, oder `LAYA_BASE_URL`
  zeigt auf den falschen Host/Port.
- **`ModuleNotFoundError: No module named 'uvicorn'`**: Das `serve`-Extra
  wurde nicht (vollständig) installiert. Erneut ausführen:
  `uv add "laya[serve]"` im Laya-Verzeichnis (Anführungszeichen nicht
  vergessen, sonst interpretiert die Shell die eckigen Klammern selbst).
- **Port belegt / alter Server läuft noch**: `pkill -f '[l]aya-serve'`.
- **Erster Request sehr langsam / scheint zu hängen**: Checkpoint-Download
  von Hugging Face läuft noch (siehe oben). Mit `LAYA_PRELOAD=1` passiert
  das bereits beim Start von `laya-serve`, nicht erst beim ersten Request.
- **"model not found" o. Ä.**: Gültige Modellnamen sind `english`,
  `multilingual`, `typed-decisions`. `LAYA_MODELS` steuert nur das Preload;
  andere Modelle werden lazy nachgeladen.

## Projektstruktur

```
ai-sttpai-laya/
├── project.scala           # scala-cli Direktiven: Scala-Version & Abhängigkeiten
├── LayaClient.scala        # JevSyncClient, baseUrl auf lokalen laya-serve umgebogen
├── Main.scala               # @main laya.main: Checkpoint "english" (Choice/Score/Noul)
├── MainMultilingual.scala  # @main laya.mainMultilingual: Checkpoint "multilingual"
├── MainTypedDecisions.scala # @main laya.mainTypedDecisions: Checkpoint "typed-decisions"
└── README.md
```
