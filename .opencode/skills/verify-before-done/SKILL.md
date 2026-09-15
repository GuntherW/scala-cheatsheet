---
name: verify-before-done
description: "Verifies formatting, linting, and tests are all green before considering a Scala coding task complete. Triggers: finishing a code change, before committing, before saying 'done'/'fertig', \"stelle sicher dass die Tests grün sind\", \"sind alle Tests grün\", \"ist der Code fertig\", after fixing a bug, after adding a feature, after a refactor. Use proactively at the end of any code-modifying task in this repo — do not wait to be asked."
---

You have just modified Scala source in this repository. Before telling the user the task is done, verify: compiles cleanly → formatted → linted → tests green, in that order, stopping at the first failure to fix it.

Use `sbt --client` for all checks (faster, connects to running sbt server). Use plain `sbt run` (never `sbt --client run`) only to start a long-running app.

## Workflow

1. **Format**
   ```bash
   sbt --client scalafmtAll
   ```
   Run first so formatting diffs don't obscure real changes later.

2. **Lint**
   ```bash
   sbt --client scalafixAll
   ```
   Fix reported violations — don't suppress/`@SuppressWarnings` unless the user explicitly approves.

3. **Compile + test — scope to what changed**

   Prefer the narrowest scope that still gives confidence:
   ```bash
   # One touched module
   sbt --client "project core" test

   # One touched test class
   sbt --client "project core" "testOnly de.codecentric.wittig.scala.futur.TestFuture"

   # One touched test method
   sbt --client "project munit" "testOnly de.wittig.CheckTest -- --test=addition*"
   ```
   If changes span multiple modules, or you're unsure what else is affected, run the full suite: `sbt --client test`.

4. **On failure**
   - Read the actual assertion/compiler error before changing anything.
   - Use `-oF` for full stack traces on a flaky/unclear failure.
   - Fix the root cause. Re-run the failing test, then the broader scope from step 3.
   - Never mark a task complete with known-red, skipped, or `.ignore`/`@Ignore`d tests — unless explicitly asked.

5. **Scala CLI files** (under `cli/`) — not part of the sbt build, verify separately:
   ```bash
   scala-cli test <file>.scala
   ```

6. **Code review** — once compile/format/lint/test are green, invoke the `scala-reviewer` subagent (via the `task`
   tool) on the `.scala` files you created or modified in this session.

   - **1–2 files changed:** one `scala-reviewer` call with the file list is enough.
   - **3+ files changed:** avoid feeding them all into a single call — attention dilutes across files (uneven depth,
     missed issues in "middle" files, contradictory findings). Instead:
     1. **Per-file pass:** launch one `scala-reviewer` task per file, in parallel (single message, multiple `task`
        calls), each scoped to exactly that file.
     2. **Integration pass:** launch one more `scala-reviewer` task, giving it the full list of changed files plus
        the concatenated findings from step 1, and ask it to check specifically for: contradictory findings between
        files (same pattern flagged in one, approved in another), cross-file data flow issues, and inconsistent API
        usage across the changed files.
   - Apply findings you agree with, then re-run the affected steps above (format/lint/compile/test) if you changed code.
   - If you disagree with a finding, say so explicitly to the user with your reasoning — don't silently drop it.
   - Treat findings with `Konfidenz` ≤0.5 as "to discuss", not as facts — mention them to the user distinctly from
     higher-confidence findings, rather than applying them silently.
   - Skip only for pure comment/doc/formatting-only edits with no logic change.

## When to skip steps

- Docs/comment-only changes: formatting/linting still apply, tests can be skipped.
- `build.sbt` / `project/Dependencies.scala` changes: run `import-build` first, then full `sbt --client test`.
- User explicitly says not to run tests yet: respect it, but flag before ending the session that verification is outstanding.

## Reporting back

State plainly which steps passed and which module/test scope was actually exercised (e.g. "ran `project core` tests only — other modules untouched"). State whether `scala-reviewer` was invoked (single-file, per-file+integration, or skipped) and summarize its findings, separating high- from low-confidence ones. Don't claim "all tests are green" if only a subset ran.
