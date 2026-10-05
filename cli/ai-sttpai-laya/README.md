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
- Python 3.10+ (für Laya)

## 1. Laya lokal installieren

```bash
mkdir -p ~/bin/laya && cd ~/bin/laya

python3 -m venv .venv
.venv/bin/python -m pip install --upgrade pip

# "serve"-Extra installiert zusätzlich FastAPI/uvicorn für laya-serve
.venv/bin/python -m pip install "laya[serve]"

# Installation prüfen (lädt noch kein Modell herunter)
.venv/bin/python -I -c "import laya; print(laya.__version__)"
```

## 2. `laya-serve` starten

`laya-serve` implementiert den Jev-kompatiblen Endpunkt `POST /v1/systemone`
sowie `GET /health`.

```bash
pushd ~/bin/laya
LAYA_HOST=127.0.0.1 LAYA_PORT=8000 LAYA_MODELS=english .venv/bin/python -m laya.serve
popd 
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

| Variable       | Bedeutung                                                                                          | Default                      |
|----------------|----------------------------------------------------------------------------------------------------|------------------------------|
| `LAYA_HOST`    | Bind-Adresse                                                                                       | `0.0.0.0`                    |
| `LAYA_PORT`    | Bind-Port                                                                                          | `8000`                       |
| `LAYA_DEVICE`  | Torch-Device für jeden Checkpoint (`cpu`, `cuda`, ...)                                             | auto                         |
| `LAYA_PRELOAD` | Checkpoints beim Start laden statt lazy beim ersten Request                                        | `1`                          |
| `LAYA_MODELS`  | Kommagetrennte Liste zu ladender Modelle (`english`,`multilingual`,`typed-decisions`); leer = alle | alle                         |
| `LAYA_API_KEY` | Bearer-Token, das Clients mitschicken müssen                                                       | nicht gesetzt (= keine Auth) |

**Sicherheitshinweis:** Ohne `LAYA_API_KEY` ist der Server komplett
unauthentifiziert. Lokal auf `127.0.0.1` ist das für einen Testlauf okay;
sobald `LAYA_HOST=0.0.0.0` (Default!) gesetzt und der Rechner im Netz
erreichbar ist, unbedingt `LAYA_API_KEY` setzen (siehe Laya `SECURITY.md`).

## 3. Dieses Scala-Projekt ausführen

```bash
cd cli/ai-sttpai-laya
scala-cli run .
```

Erwartete Ausgabe (Werte können je nach Modell leicht variieren):

```
Department: billing (confidence 0.94)
Urgency score: 1.42 -> soon
Churn risk probability: 0.86
Model used: english, requestId: None
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
- **Erster Request sehr langsam / scheint zu hängen**: Checkpoint-Download
  von Hugging Face läuft noch (siehe oben). Mit `LAYA_PRELOAD=1` passiert
  das bereits beim Start von `laya-serve`, nicht erst beim ersten Request.
- **"model not found" o. Ä.**: Gültige Modellnamen sind `english`,
  `multilingual`, `typed-decisions` - und müssen zusätzlich über
  `LAYA_MODELS` beim Start von `laya-serve` freigegeben worden sein.

## Projektstruktur

```
ai-sttpai-laya/
├── project.scala     # scala-cli Direktiven: Scala-Version & Abhängigkeiten
├── LayaClient.scala  # JevSyncClient, baseUrl auf lokalen laya-serve umgebogen
├── Main.scala        # @main, Beispiel-Request (Choice/Score/Noul)
└── README.md
```
