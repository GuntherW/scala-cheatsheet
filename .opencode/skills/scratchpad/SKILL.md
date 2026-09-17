---
name: scratchpad
description: "Maintains a SCRATCHPAD.md file with concrete findings (class names, exact file:line paths, dependency chains) so they survive context degradation, /compact, and session restarts. Triggers: exploring an unfamiliar codebase/module, tracing a call chain or dependency across services, a task touching 3+ files, a refactor spanning multiple files, resuming an ongoing task after /compact or in a new session, 'lies dir den Kontext nochmal durch', 'wo waren wir stehengeblieben'. Use proactively at the START of such tasks — do not wait until the answers start sounding generic ('typical pattern', 'usual repository structure')."
---

You are about to do extended exploration or a multi-file task. Verbose tool output (file reads, greps, directory
listings) will accumulate in the conversation and eventually push earlier, precise findings out of attention. The
observable symptom: you start saying "this follows the typical repository pattern" instead of naming the actual
class, file, and line. `SCRATCHPAD.md` prevents this by persisting findings outside the conversation context.

## When to create it

At the **start** of the task, not once things feel degraded — check for `SCRATCHPAD.md` in the repo root (or the
relevant module root for `cli/` sub-projects) before doing any exploration. If missing, create it immediately.

## Structure

```markdown
# Ziel
<one line: what this task is trying to achieve>

## Erkenntnisse & Architektur
- `OrderRepository` (src/repos/order.ts:1) — implements Repository<T>, custom findById caching
- `OrderService` (src/services/order.ts:14) — orchestrates OrderRepository + PaymentGateway
- RefundProcessor → OrderService → OrderRepository → PostgreSQL

## Checkliste
- [x] Trace refund flow from controller to DB
- [ ] Investigate PaymentGateway error handling
- [ ] Check cache invalidation on status change
```

## Content rules (this is the point of the whole exercise)

- Write **specific identifiers**: exact class/method/function names with `file:line`, not descriptions of what a
  layer "usually" does.
- Write **dependency chains** as `A → B → C`, not prose paraphrases.
- One bullet per discrete finding — don't batch multiple discoveries into one vague sentence.
- If you can't name the specific symbol/file yet, that's a signal to go look, not to write a placeholder summary.

## When to write

Update `## Erkenntnisse & Architektur` and `## Checkliste` **immediately after each significant finding** — right
after reading the file or running the search that produced it, not batched at the end of the session. Treat it like
note-taking during the investigation, not a final report.

## When to read

- At the start of every session/turn continuing this task.
- Right after `/compact`.
- Before delegating to a subagent via the `task` tool — inject the relevant scratchpad bullets into the subagent's
  prompt instead of letting it rediscover the same things.

## Lifecycle

Delete `SCRATCHPAD.md` only once all tests are green and the task is fully done. Don't delete it just because a
subtask finished — the file spans the whole task, including work resumed after a restart or `/compact`.
