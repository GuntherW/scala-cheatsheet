# AGENTS.md - Coding Agent Instructions

* use `sbt --client` instead of `sbt` to connect to a running sbt server for faster execution
* to verify that the app starts use `sbt run`, WITHOUT  `--client`, as it prevents interrupting the process
* ALWAYS use tools to compile and run tests instead of relying on bash commands
* after adding a dependency to `build.sbt`, ALWAYS run the  `import-build` tool
* to lookup a dependency or the latest version, use the  `find-dep` tool
* to lookup the API of a class, use the `inspect` tool

## Project Structure

Multi-module Scala 3 monorepo with SBT-based modules (`core`, `zio`, `http4s`, etc.) and Scala CLI projects in `cli/`.

## Build Commands

### SBT Projects

### Scala CLI Projects (cli/ subdirectory)

```bash
scala-cli run <file>.scala            # Run file
scala-cli run <file>.scala --watch    # Watch mode
scala-cli package <file>.scala -o az  # Package executable
scala-cli setup-ide .                 # IDE setup
scala-cli dependency-update . --all   # Update deps
```

## Lint Commands

```bash
sbt --client scalafmtAll                       # Format all code
sbt --client scalafmtCheckAll                  # Check formatting
sbt --client scalafixAll                       # Run scalafix linting
```

## Test Commands

```bash
sbt --client test                              # All tests
sbt --client "project zio" test                # Specific project

# Single test class
sbt --client "project core" "testOnly de.codecentric.wittig.scala.futur.TestFuture"

# Single test method (MUnit/ScalaTest)
sbt --client "project munit" "testOnly de.wittig.CheckTest -- --test=addition*"

# Verbose output
sbt --client "project core" "testOnly de.wittig.SomeTest -- -oF"

# Scala CLI tests
scala-cli test <file>.scala
```

## Code Style

### Scala 3 Formatting

- Max column: 200, alignment: `most`, preserve newlines/trailing commas
- Wildcard imports: `*` not `_`

### Naming Conventions

| Element        | Convention | Example         |
|----------------|------------|-----------------|
| Classes/Traits | PascalCase | `BlobService`   |
| Objects        | PascalCase | `BlobViewerApp` |
| Methods/Values | camelCase  | `loadBlobs`     |
| Case classes   | PascalCase | `BlobInfo`      |
| Packages       | lowercase  | `de.wittig`     |

### Logging

```scala
// Use println for simple CLI, logging framework for server apps
println(s"Loading blobs: $containerName")
// Or usezio-logging for ZIO applications
```

### Main Entry Points

```scala
// Scala CLI / Scala 3
@main
def mainLocalCli(): Unit = BlobViewerApp.run()

// SBT - add to build.sbt
Compile / mainClass := Some("de.wittig.myapp.Main")
```

## Dependencies

### SBT (build.sbt)

```scala
lazy val myProject = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(Library.someLibrary, Library.testLibrary % Test),
  )
```

### Scala CLI (directives at file top)

```scala
//> using dep com.azure:azure-storage-blob:12.32.0
//> using file AzureBlobService.scala
```

## Test Frameworks

| Framework  | Usage                                           |
|------------|-------------------------------------------------|
| MUnit      | `extends FunSuite` or `extends ScalaCheckSuite` |
| ZIO Test   | `extends ZIOSpecDefault`                        |
| ScalaTest  | `extends AnyFunSuite`                           |
| ScalaCheck | Property-based testing                          |

Test location: `src/test/scala/<package>/`

## Scalafix Rules (Enabled)

- `LeakingImplicitClassVal`: Detect leaking implicit class vals
- `DisableSyntax`: Enabled with strict rules (commented out in .scalafix.conf but available):
    - No vars, no returns, no while loops, no asInstanceOf
    - No isInstanceOf, no xml, no val patterns
    - No universal equality (== and != are unsafe)
- `OrganizeImports`: Available with Scala3 dialect

## Scalafmt Configuration

```bash
version = 3.11.5
maxColumn = 200
align.preset = most
runner.dialect = scala3
newlines.source = keep
trailingCommas = preserve
```

## Project Modules (SBT)

Core: `core`, `zio`, `zioHttp`, `zioKafka`, `zioSchema`, `http4s`, `sttp`, `tapir`, `caliban`
Data: `database`, `mongo`, `kafka`, `datatransformation`, `parsers`
Testing: `munit`, `scalacheck`, `gatling`, `cucumber`
Other: `akka`, `json`, `macros`, `macwire`, `magnolia`, `scalajs`, `spring`, `grpcFs2`, `osLib`, `openAI`, `cdk`,
`direct`, `config`

## Important Files

- `build.sbt` - Main build config (commonSettings, version, compiler options)
- `.scalafmt.conf` - Formatting rules (version 3.10.7, Scala 3 dialect)
- `.scalafix.conf` - Linting rules
- `project/Dependencies.scala` - Dependency versions

## Gotchas

1. Forked tests (`fork := true`) for parallelism - required for most projects
2. `scalafixOnCompile := true` - runs linting on compile
3. Scala CLI requires `//> using dep` directives at file top
4. Tests use `-oF` flag for full stack traces
5. Semanticdb enabled for IDE support

## Scratchpad Guidelines

Für komplexe, mehrstufige Aufgaben (mehr als 3 Dateien oder Refactorings) gilt:

- **Initialisierung:** Prüfe, ob `SCRATCHPAD.md` existiert. Wenn nicht, erstelle sie.
- **Read First:** Lies zu Beginn einer Session oder nach einer Kontext-Kompression (`/compact`) immer als Erstes die
  `SCRATCHPAD.md`.
- **Write Incrementally:** Halte die Datei mit folgenden Sektionen aktuell:
    - `# Ziel` (Kurze Beschreibung der Aufgabe)
    - `## Erkenntnisse & Architektur` (Wichtige Pfade, Modul-Abhängigkeiten)
    - `## Checkliste` (Aufgaben mit `- [ ]` und `- [x]`)
- **Cleanup:** Lösche die `SCRATCHPAD.md` erst, wenn alle Tests grün sind und die Aufgabe vollständig abgeschlossen ist.