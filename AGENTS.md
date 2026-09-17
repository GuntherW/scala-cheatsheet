# AGENTS.md - Coding Agent Instructions

* Use `sbt --client` for compile/test/lint (faster, uses running server)
* Exception: `sbt run` (no `--client`) to start a long-running app — `--client` would let it be interrupted
* Prefer tools over bash for compile/test where available
* After adding a dependency in `build.sbt`, run `import-build`
* To look up a dependency/version, use `find-dep`; for a class API, use `inspect`
* For Scala/Java symbol navigation (definitions, references/usages, implementations, hierarchies, "who calls X",
  before renames/refactors) ALWAYS load the `scalex` skill FIRST — do not reach for `grep`/`rg`/bash text search on
  `.scala`/`.java` files until `scalex` has been tried and was insufficient (untracked files, literal string
  search, non-JVM files are fine to grep directly)

## Project Structure

Multi-module Scala 3 monorepo: SBT modules (`core`, `zio`, `http4s`, etc.) + Scala CLI projects in `cli/` (see
`cli/AGENTS.md`).

## Commands

```bash
# Lint
sbt --client scalafmtAll                       # format all
sbt --client scalafixAll                       # lint

# Test
sbt --client test                              # all
sbt --client "project zio" test                # one module
sbt --client "project core" "testOnly de.codecentric.wittig.scala.futur.TestFuture"        # one class
sbt --client "project munit" "testOnly de.wittig.CheckTest -- --test=addition*"             # one method
sbt --client "project core" "testOnly de.wittig.SomeTest -- -oF"                            # full stack traces
```

## Code Style

- Scalafmt: maxColumn 200, `align.preset = most`, dialect scala3 (see `.scalafmt.conf`)
- Wildcard imports: `*` not `_`
- Logging: `println` for CLI, zio-logging for ZIO apps

| Element        | Convention | Example         |
|----------------|------------|-----------------|
| Classes/Traits | PascalCase | `BlobService`   |
| Objects        | PascalCase | `BlobViewerApp` |
| Methods/Values | camelCase  | `loadBlobs`     |
| Case classes   | PascalCase | `BlobInfo`      |
| Packages       | lowercase  | `de.wittig`     |

```scala
// Main entry point (Scala CLI / Scala 3)
@main def mainLocalCli(): Unit = BlobViewerApp.run()

// SBT
Compile / mainClass := Some("de.wittig.myapp.Main")
```

## Dependencies

```scala
// SBT (build.sbt)
lazy val myProject = project
  .settings(commonSettings, libraryDependencies ++= Seq(Library.someLibrary, Library.testLibrary % Test))
```

## Test Frameworks

| Framework  | Usage                                           |
|------------|--------------------------------------------------|
| MUnit      | `extends FunSuite` or `extends ScalaCheckSuite` |
| ZIO Test   | `extends ZIOSpecDefault`                        |
| ScalaTest  | `extends AnyFunSuite`                           |
| ScalaCheck | Property-based testing                          |

Location: `src/test/scala/<package>/`

## Scalafix Rules (enabled)

- `LeakingImplicitClassVal`
- `DisableSyntax`: no `var`/`return`/`while`/`asInstanceOf`/`isInstanceOf`/xml/val-patterns/`==`,`!=` (universal equality) — some rules commented out in `.scalafix.conf`, check before assuming active
- `OrganizeImports` (Scala3 dialect)

## Project Modules (SBT)

Core: `core`, `zio`, `zioHttp`, `zioKafka`, `zioSchema`, `http4s`, `sttp`, `tapir`, `caliban`
Data: `database`, `mongo`, `kafka`, `datatransformation`, `parsers`
Testing: `munit`, `scalacheck`, `gatling`, `cucumber`
Other: `akka`, `json`, `macros`, `macwire`, `magnolia`, `scalajs`, `spring`, `grpcFs2`, `osLib`, `openAI`, `cdk`, `direct`, `config`

## Important Files

- `build.sbt` — commonSettings, version, compiler options
- `.scalafmt.conf` / `.scalafix.conf` — format/lint rules
- `project/Dependencies.scala` — dependency versions

## Gotchas

1. `fork := true` — required for most projects (parallel tests)
2. `scalafixOnCompile := true` — lint runs on compile
3. Semanticdb enabled for IDE support

## Scratchpad

For codebase exploration or tasks touching 3+ files: load the `scratchpad` skill FIRST, before starting — it
maintains `SCRATCHPAD.md` with concrete findings so they survive context degradation and `/compact`.
