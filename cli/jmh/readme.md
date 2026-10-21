### Ausführung

```shell 
scala --jmh CompactObjectHeaders.scala
```

```shell
scala --jmh SearchCacheBenchmark.scala
```

Mit Speicherverbrauch:

```shell
scala-cli --jmh SearchCacheBenchmark.scala -- -prof gc
```

Die Datei enthält zwei Benchmarks:

- `allocateWithoutCompactHeaders` (`-XX:-UseCompactObjectHeaders`)
- `allocateWithCompactHeaders` (`-XX:+UseCompactObjectHeaders`)

Beide Varianten werden über `@Fork(... jvmArgsAppend = ...)` getrennt gestartet und sind damit direkt vergleichbar.

CPU vs. GPU (Cyfra, benötigt Vulkan):

```shell
scala-cli --jmh CpuVsGpuBenchmark.scala
```
