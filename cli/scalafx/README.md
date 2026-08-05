# ScalaFX Demo – Packaging

## Ausführen (ohne Packaging)

```bash
cd cli/scalafx && scala-cli run Main.scala
```

---

## Packaging-Varianten

### 1. Executable JAR (fat JAR mit Preamble)

Erzeugt eine einzelne ausführbare Datei, die intern ein JAR ist, aber direkt aufgerufen werden kann (funktioniert auf
Linux/macOS ohne `java -jar`):

```bash
cd cli/scalafx && scala-cli --power package --assembly --preamble -o scalafx-demo Main.scala
```

```bash
cd cli/scalafx && ./scalafx-demo
```

- `--assembly` – bündelt alle Dependencies in eine Datei
- `--preamble` – fügt ein Bash-Skript vorne an, das `java -jar` intern aufruft
- `-o` – Name der Ausgabedatei

> **Hinweis:** Java muss auf dem Zielsystem installiert sein.


