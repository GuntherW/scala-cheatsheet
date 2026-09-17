---
name: cellar
description: >
  Look up the public API of any JVM dependency (Scala 3, Scala 2, Java) from
  the terminal — type signatures, members, docs, and source as Markdown, no
  JAR unpacking needed. Use this skill whenever you need to call an unfamiliar
  library method, explore a package's types, or check a dependency's API.
  Works standalone from the terminal — no IDE/Metals dependency needed.
---

# Cellar

Look up JVM dependency APIs from the terminal. NEVER download or unpack JARs manually.

## Project-aware (run from project root)

Queries the current project and its dependencies. Auto-detects Mill, sbt, scala-cli.

```sh
cellar get [--module <name>] <fqn>       # signature, members, docs
cellar list [--module <name>] <package>  # symbols in a package or class
cellar search [--module <name>] <query>  # case-insensitive substring search
```

- Mill/sbt: `--module` required (e.g. `--module core`)
- scala-cli: omit `--module`
- `--no-cache` — re-extract classpath from the build tool
- `--java-home <path>` — override JRE classpath
- `-l N` — limit `list`/`search` results or `get` members
- `--hide-inherited` / `--group-inherited` — control inherited members on `get`

## External (any Maven coordinate)

```sh
cellar get-external <coordinate> <fqn>
cellar list-external <coordinate> <package>
cellar search-external <coordinate> <query>
cellar get-source <coordinate> <fqn>
cellar deps <coordinate>
cellar meta <coordinate>
```

- Coordinates must be explicit: `group:artifact_3:version` (no `::` shorthand)
- sbt plugins: full Scala + sbt suffix, e.g. `org.scala-native:sbt-scala-native_2.12_1.0:latest`
- Full Scala-version artifacts: e.g. `group:artifact_3.3.8:version`
- Version may be `latest`

## Workflow

1. Package/name unknown → `search` / `search-external`
2. Package or enclosing class known, type unknown → `list` / `list-external`
3. Type known → `get` / `get-external`
4. Implementation needed → `get-source`
5. Coordinate/POM only → `meta` or `deps`

## Examples

```sh
cellar get-external org.typelevel:cats-core_3:2.10.0 cats.Monad
cellar get-external --hide-inherited org.typelevel:cats-core_3:2.10.0 cats.Monad
cellar list-external io.circe:circe-core_3:0.14.6 io.circe
cellar search-external org.typelevel:cats-core_3:2.10.0 flatMap
cellar get-source org.typelevel:cats-core_3:2.10.0 cats.Monad
cellar deps org.typelevel:cats-effect_3:3.5.4
cellar get --module lib cats.Monad
```

## Scope

cellar reads APIs only — signatures, members, docs, source.
For project navigation (definitions, usages, hierarchy) use `scalex` instead.
For compile/test/format use `sbt --client` (see AGENTS.md).