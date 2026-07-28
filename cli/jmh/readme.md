### Ausführung

```shell 
scala-cli --jmh CompactObjectHeaders.scala
```

Die Datei enthält zwei Benchmarks:

- `allocateWithoutCompactHeaders` (`-XX:-UseCompactObjectHeaders`)
- `allocateWithCompactHeaders` (`-XX:+UseCompactObjectHeaders`)

Beide Varianten werden über `@Fork(... jvmArgsAppend = ...)` getrennt gestartet und sind damit direkt vergleichbar.
