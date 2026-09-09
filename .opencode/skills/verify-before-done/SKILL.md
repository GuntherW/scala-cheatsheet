---
name: verify-before-done
description: "Verifies formatting, linting, and tests are all green before considering a Scala coding task complete. Triggers: finishing a code change, before committing, before saying 'done'/'fertig', \"stelle sicher dass die Tests grün sind\", \"sind alle Tests grün\", \"ist der Code fertig\", after fixing a bug, after adding a feature, after a refactor. Use proactively at the end of any code-modifying task in this repo — do not wait to be asked."
---

You have just modified Scala source in this repository. Before telling the user the task is done, verify the change compiles cleanly, is formatted, passes linting, and all relevant tests are green — in that order, stopping at the first failure to fix it.

Use `sbt --client` for all checks below (faster, connects to the running sbt server). Only use plain `sbt run` (never `sbt --client run`) when the goal is to start a long-running app, since `--client` would let it be interrupted.

## Workflow

1. **Format**
   ```bash
   sbt --client scalafmtAll
   ```
   Formats all code. Run this first so formatting diffs don't obscure real changes in later steps.

2. **Lint**
   ```bash
   sbt --client scalafixAll
   ```
   Runs scalafix rules (`LeakingImplicitClassVal`, `DisableSyntax`, `OrganizeImports`). Fix any reported violations before moving on — do not suppress or `@SuppressWarnings` around them unless the user explicitly approves.

3. **Compile + test — scope to what changed**

   Prefer the narrowest scope that still gives confidence, to keep feedback fast:
   ```bash
   # One touched module
   sbt --client "project core" test

   # One touched test class
   sbt --client "project core" "testOnly de.codecentric.wittig.scala.futur.TestFuture"

   # One touched test method
   sbt --client "project munit" "testOnly de.wittig.CheckTest -- --test=addition*"
   ```
   If changes span multiple modules, or you are not sure what else might be affected, run the full suite:
   ```bash
   sbt --client test
   ```

4. **On failure**
   - Read the actual assertion/compiler error before changing anything — don't guess.
   - Use `-oF` for full stack traces on a flaky/unclear failure: `sbt --client "project core" "testOnly de.wittig.SomeTest -- -oF"`.
   - Fix the root cause. Re-run only the failing test first, then re-run the broader scope from step 3 to confirm nothing else broke.
   - Never mark a task complete with known-red tests, skipped tests, or `.ignore`/`@Ignore` added to silence a failure — unless the user explicitly asked for that.

5. **Scala CLI files** (under `cli/`)
   These are not part of the sbt build — verify them separately:
   ```bash
   scala-cli test <file>.scala
   ```

## When to skip steps

- Pure documentation/comment-only changes: formatting/linting still apply, tests can be skipped.
- `build.sbt` / `project/Dependencies.scala` changes: run `import-build` first (per AGENTS.md), then do a full `sbt --client test` since dependency bumps can break anything.
- If the user is mid-exploration and explicitly says not to run tests yet, respect that — but flag before ending the session that verification is still outstanding.

## Reporting back

State plainly which steps passed and which module/test scope was actually exercised (e.g. "ran `project core` tests only — other modules untouched"). Do not claim "all tests are green" if only a subset was run; be specific about scope.
