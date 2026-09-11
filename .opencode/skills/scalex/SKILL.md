---
name: scalex
description: "Scala code intelligence CLI for Scala codebases. Find definitions, implementations, usages, imports, members, scaladoc, codebase overview, package API surface, files, annotated symbols, file contents. Triggers: \"where is X defined\", \"who implements Y\", \"find usages of Z\", \"what methods does X have\", \"show source of X\", \"inheritance tree\", \"explain this type\", \"what changed since commit\", \"find types extending X with method Y\", \"what does this package export\", or before renaming. Test navigation: \"what tests exist\", \"is X tested\", \"show test for Y\", \"find tests covering Z\". Use proactively exploring unfamiliar Scala code. Supports fuzzy camelCase search (e.g. \"hms\" finds HttpMessageService). Prefer scalex over grep/glob for Scala symbol lookups. Use `scalex grep` for .scala content search — integrates with --path and --no-tests filters."
---

`scalex` is a Scala code intelligence CLI — parses Scala/Java via Scalameta, no compiler/build server needed. Works
with Scala 3 and Scala 2 (falls back to 2.13 dialect). Only works on **git-tracked files**.

First run indexes all tracked `.scala`/`.java` files (~3s/14k files); later runs re-parse only changed files
(~400-500ms) via OID caching. Java files indexed via regex (class/interface/enum/record).

## Invocation

`scalex <command> [args]`. Defaults to cwd; use `-w /path` to set workspace (preferred over positional to avoid
ambiguity). Auto-indexes on first run.

## What's indexed

Top-level declarations only: classes, traits, objects, enums, defs, vals, types, named givens (anonymous givens
skipped), extension groups, annotations. NOT indexed: local defs inside method bodies, params, pattern bindings.

`refs`, `imports`, `grep` are plain text search across files (word-boundary/regex) — find everything regardless of
index.

## Commands

### `scalex def <symbol> [--verbose] [--kind K] [--no-tests] [--path PREFIX]`
Find definition, incl. `given` instances. `--verbose` shows full signature inline. Ranked: class/trait/object/enum
first, non-test before test, shorter paths first. Supports package-qualified (`def com.example.Cache`, partial
`def cache.Cache`) and `Owner.member` dotted syntax (`def PaymentService.processPayment`).

### `scalex impl <trait> [--verbose] [--kind K] [--no-tests] [--path PREFIX] [--limit N]`
Classes/objects/enums extending or mixing in a trait. Also finds type-param usage in extends clauses (`class Bar
extends Mixin[Foo]`). Faster/more targeted than `refs` for concrete implementations.

### `scalex refs <symbol> [--flat] [--count] [--top N] [--strict] [--category CAT] [--no-tests] [--path PREFIX] [-C N] [--limit N]`
Word-boundary text search, bloom-filter accelerated. 20s timeout (large codebases may return partial results).
Categorized by default: Definition/ExtendedBy/ImportedBy/UsedAsType/Usage/Comment. `--count` for triage without full
lists, `--top N` ranks files by ref count, `-C N` for grep-style context, `--flat` for a flat list.

### `scalex imports <symbol> [--strict] [--no-tests] [--path PREFIX] [--limit N]`
Import statements only — cleaner than `refs` for dependency analysis. 20s timeout.

### `scalex members <symbol> [--verbose] [--brief] [--body] [--max-lines N] [--inherited] [--kind K] [--no-tests] [--path PREFIX] [--limit N]`
Member declarations (def/val/var/type) of a class/trait/object/enum. Parsed on-the-fly, not indexed. Companion-aware
(shows companion members automatically). `--inherited` walks the extends chain, marks shadowing members
`[override]`. `--body` inlines method bodies; `--max-lines N` caps inlined size (0 = unlimited).

### `scalex doc <symbol> [--kind K] [--no-tests] [--path PREFIX] [--limit N]`
Leading scaladoc comment. Returns "(no scaladoc)" if none.

### `scalex search <query> [--kind K] [--verbose] [--limit N] [--exact] [--prefix] [--definitions-only] [--returns TYPE] [--takes TYPE]`
Fuzzy name search: exact > prefix > substring > camelCase abbreviation (`hms` → `HttpMessageService`). Ranked by
import popularity. `--exact`/`--prefix` cut noise on large codebases. `--definitions-only` restricts to
class/trait/object/enum. `--returns`/`--takes` filter by substring match on return type / parameter types.

### `scalex grep <pattern> [--in <symbol>] [-e PAT]... [--count] [--no-tests] [--path PREFIX] [-C N] [--limit N]`
Regex (Java, not POSIX — `|` not `\|`, `( )` not `\( \)`) content search across `.scala` files, with built-in
`--path`/`--no-tests` filtering. Prefer over the Grep tool for `.scala` files. `-e` combines multiple patterns
(OR'd). `--in <symbol>` scopes to a class/method body (supports `Owner.member`). 20s timeout.

### `scalex body <symbol> [--in <owner>] [-C N] [--imports] [--no-tests] [--path PREFIX] [--limit N]`
Full source body via Scalameta spans — def/val/var/type/class/trait/object/enum. `--in <owner>` disambiguates
same-named members across classes. `--imports` prepends the file's import block. Also extracts test bodies by exact
test-name string (`test("...")`, `it("...")`, `describe("...")`, `"..." in { }`, `"..." >> { }`); scope with
`--in SuiteName`.

### `scalex hierarchy <symbol> [--up] [--down] [--depth N] [--no-tests] [--path PREFIX]`
Inheritance tree via extends clauses. `--up` parents only, `--down` children only, `--depth N` limits tree depth
(default: no cap). External/unknown parents shown as `[external]`.

### `scalex overrides <method> [--of <trait>] [--body] [--max-lines N] [--limit N]`
All implementations of a method name across types (or scoped `--of <trait>`). `--body`/`--max-lines N` inline
source.

### `scalex explain <symbol> [--verbose] [--brief] [--body] [--max-lines N] [--shallow] [--no-doc] [--inherited] [--impl-limit N] [--members-limit N] [--expand N] [--no-tests] [--path PREFIX] [--exclude-path PREFIX]`
One-shot composite: definition + scaladoc + members (top 10) + companion + implementations (top 5) + import files —
saves 4-5 round-trips. Supports package-qualified and `Owner.member` syntax. `--shallow` = def+members+companion
only. `--brief` = def + top 3 members only (pairs well with `batch`). `--expand N` recursively expands each impl's
members. `--inherited` merges parent members with provenance. Fuzzy fallback on miss; falls back to `summary` on
package match. Multiple matches → prints ready-to-run `scalex explain pkg.Name` on stderr.

### `scalex tests [<pattern>] [--verbose] [--path PREFIX] [--json]`
Test names from MUnit/ScalaTest/specs2 conventions, test files only. Passing `<pattern>` filters by substring **and
shows full bodies inline** — fastest way to find+read a specific test.

## Additional commands

Run `scalex <command> --help` for full flags.

| Command               | Purpose                                                          | Key flags                         |
|------------------------|-------------------------------------------------------------------|------------------------------------|
| `overview`            | Codebase summary: symbols by kind, top packages, hub types       | `--architecture`, `--focus-package` |
| `file <query>`        | Find files by name (fuzzy)                                       |                                    |
| `annotated <ann>`     | Symbols with a given annotation                                  | `--kind K`                        |
| `package <pkg>`       | All symbols in a package, by kind                                | `--definitions-only`, `--verbose` |
| `api <pkg>`           | Public API surface (externally imported symbols)                | `--used-by PKG`                   |
| `summary <pkg>`       | Sub-packages with symbol counts                                  |                                    |
| `deps <symbol>`       | What a symbol depends on (reverse of `refs`)                     | `--depth N`                       |
| `context <file:line>` | Enclosing scopes at a line                                       |                                    |
| `diff <git-ref>`      | Symbol-level diff vs a git ref                                   |                                    |
| `ast-pattern`         | Structural search                                                | `--extends`, `--has-method`, `--body-contains` |
| `entrypoints`         | `@main`, `def main`, `extends App`, test suites                  | `--no-tests`                      |
| `coverage <symbol>`   | References in test files only (is this tested?)                 |                                    |
| `batch`               | Multiple queries, one index load (stdin)                        |                                    |
| `symbols <file>`      | What's defined in this file                                     | `--summary`                       |
| `packages`            | List all packages                                                |                                    |
| `index`               | Force reindex (rarely needed)                                    |                                    |

## Non-obvious workflows

- **3+ lookups at once** → `batch`: `echo -e "def Foo\nimpl Foo\nrefs Foo" | scalex batch -w /project`
- **Full API + impls in one call** → `explain MyTrait` (`--expand 1` for impl members, `--brief` for condensed)
- **Common-name disambiguation** → `def com.example.cache.Cache` (package-qualified or partial)
- **Coupling analysis** → `api com.example --used-by com.example.web`
- **All output as JSON** → append `--json` to any command

## Fallback

"Not found" → symbol may be local (not top-level), in a file with parse errors, or not git-tracked. Fall back to
Grep/Glob/Read.

## Why scalex over grep

Understands Scala syntax — finds `given`/`enum`/`extension` and annotated symbols grep misses. Structured output
(kind, package, line). Categorized `refs` gives refactoring-ready impact analysis in one pass. `grep` subcommand adds
`--path`/`--no-tests` filtering grep/Grep tool lack.
