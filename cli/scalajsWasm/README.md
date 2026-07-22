# Minimales Mill Scala/Scala.js Webprojekt

Dieses Projekt ist ein minimales Beispiel mit:

- Scala-JVM Backend (`Tapir` + `Netty Sync` + `OX`)
- Scala.js Frontend (`scalajs-dom`)
- Integration wie im Mill-Web-Beispiel (`client.fastLinkJS` wird als statische Ressource ausgeliefert)

## Starten

```bash
mill run
```

Danach im Browser öffnen: <http://localhost:8080>

OpenAPI / Swagger UI: <http://localhost:8080/docs>

## Nützliche Kommandos

```bash
mill __.compile
mill client.fastLinkJS
mill clean run
```
