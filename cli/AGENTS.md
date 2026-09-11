# AGENTS.md - cli/ (Scala CLI Projects)

Standalone Scala CLI scripts — not part of the sbt build. Each project uses `//> using` directives at the file top
instead of `build.sbt`.

## Commands

```bash
scala-cli run <file>.scala            # run
scala-cli run <file>.scala --watch    # watch mode
scala-cli test <file>.scala           # test
scala-cli package <file>.scala -o az  # package executable
scala-cli setup-ide .                 # IDE setup
scala-cli dependency-update . --all   # update deps
```

## Dependencies & main entry point

```scala
//> using dep com.azure:azure-storage-blob:12.32.0
//> using file AzureBlobService.scala

@main def mainLocalCli(): Unit = BlobViewerApp.run()
```

## Gotchas

- `//> using dep` directives are required at the file top — no `build.sbt`/`Dependencies.scala` to fall back on.
