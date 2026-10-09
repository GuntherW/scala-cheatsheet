# Laya & openjev lokal ausprobieren

Diese Datei enthält die Schritte, um Laya (NandhaKishorM/laya) und openjev.com (SemIf)
lokal auszuprobieren.

## Systemvoraussetzungen (Stand der Prüfung)

- GPU: NVIDIA RTX 2000 Ada, 8 GB VRAM
- RAM: 61 GB (davon ~41 GB frei)
- OS: Ubuntu 24.04 (KDE neon)
- Ollama installiert: v0.15.5 (veraltet, aktuell ist 0.35.x / 0.40.x)
- Fazit: Beide Projekte benötigen KEIN Ollama. Ollama-Update ist unabhängig davon empfehlenswert.

---

## 1. Laya installieren (Python-Paket, lokal via PyTorch)

Voraussetzung: Python 3.10 oder neuer.

```bash
# Projektverzeichnis für das Experiment anlegen
mkdir -p ~/bin/laya
cd ~/bin/laya

# virtuelle Umgebung anlegen
python3 -m venv .venv
.venv/bin/python -m pip install --upgrade pip

# Laya installieren
.venv/bin/python -m pip install laya

# Installation prüfen (lädt noch kein Modell)
.venv/bin/python -I -c "import laya; print(laya.__version__)"
```

### Optional: GPU-Unterstützung sicherstellen

Standard-PyPI-Wheel von PyTorch sollte CUDA bereits unterstützen. Prüfen mit:

```bash
.venv/bin/python -c "import torch; print(torch.cuda.is_available())"
```

Falls `False`: passenden CUDA-Build von PyTorch gemäß
https://pytorch.org/get-started/locally/ installieren, bevor `laya` erneut installiert wird.

### Laya per CLI testen (kein Download nötig für reines Routing)

```bash
.venv/bin/laya "I was charged twice, please refund"
```

### Laya mit echter Vorhersage testen (lädt Checkpoint von Hugging Face herunter)

```bash
.venv/bin/laya "Refactor this service" --predict
```

### Laya per Python-Skript nutzen

```python
from laya import Router

router = Router()  # lädt Checkpoint beim ersten Aufruf herunter
state = "Hi, we were billed twice for March. Please refund the duplicate today."
questions = {
    "department": {
        "type": "choice",
        "instructions": "Which department should handle this?",
        "criteria": {
            "billing": "invoices, payments, refunds",
            "technical": "bugs, outages, system errors",
            "other": "everything else",
        },
    },
}
result = router.predict(state, questions)
print(result["answers"]["department"]["choice"])
```

### Optional: Web-GUI + JSON-API lokal starten

```bash
.venv/bin/python -m pip install "laya[serve]"
.venv/bin/python examples/server.py   # falls lokaler Checkout vorhanden
# Browser öffnen: http://127.0.0.1:8000
```

(Hinweis: `examples/server.py` setzt einen lokalen Git-Checkout des Repos voraus,
nicht nur das pip-Paket. Für den Checkout: `git clone https://github.com/NandhaKishorM/laya.git`)

---

## 2. openjev.com (SemIf) im Browser ausprobieren

Keine Installation nötig. Läuft komplett lokal im Browser via WebGPU.

1. Browser mit WebGPU-Unterstützung verwenden (aktuelles Chrome oder Edge empfohlen;
   Firefox experimentell/ggf. Flag nötig)
2. Seite öffnen: https://openjev.com/
3. Modell auswählen:
    - Qwen3 0.6B (639 MB) – für schwächere Geräte
    - MiniCPM5 2B (1.56 GB) – empfohlen für Desktop (Standardauswahl)
    - Qwen3.5 4B (3.01 GB) – für leistungsfähigere Desktops
4. "Load the model once" klicken, warten bis Download + Warmup abgeschlossen sind
5. Im Abschnitt "Give it a real choice" eigenen State, Frage und Optionen eingeben
   oder eines der Beispiele (Account support / Email triage) nutzen
6. "run both methods" klicken, um direkten Logit-Readout vs. Token-für-Token-JSON-
   Generierung zu vergleichen

Hinweis: Eingaben verlassen laut Seite den Browser nicht; Modelle werden im
Browser-Cache gehalten.

---

## Offene Punkte / später zu klären

- Welche konkrete Aufgabe/Use-Case soll mit Laya oder openjev gelöst werden?
- Falls Fine-Tuning gewünscht: siehe Laya-Notebook
  `notebooks/laya_finetune_typed_decisions_2xT4_kaggle.ipynb` im Repo.
  </content>
