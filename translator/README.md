# translator

Java NeoForge → Bedrock Add-On translator. Standalone Kotlin/JVM tool, not a
NeoForge mod.

## Quick start

```sh
./gradlew :translator:translate                          # all mods
./gradlew :translator:translate --args="securityguard"   # one mod
./gradlew :translator:test                               # full test suite
```

Output lands under `bedrock-out/<modId>/` and is committed to the repo.

## CLI flags

| Flag             | Phase | Behavior                                                                 |
|------------------|-------|--------------------------------------------------------------------------|
| _no flag_        | 0–3   | Default. JSON pipeline + Java pipeline + cache-only LLM stage            |
| `--no-llm`       | 3     | Alias for default. Recognized for forward-compat with older CI scripts   |
| `--with-llm`     | 3     | Enable live Anthropic API calls. Requires `ANTHROPIC_API_KEY` in env     |
| `--clear-cache`  | 3     | Wipe `translator/.cache/llm/` before running                             |
| `--diff`         | 4+    | Not yet implemented (exits 2)                                            |

## Phase 3: LLM stage

Phase 2b emits Bedrock entity / item JSON for vanilla AI goals (catalog hit).
Custom goals — `StunningMeleeGoal`, `OfferFlowerGoal`, `GuardTargetHostilesGoal`,
`BlackjackStrikeGoal`, etc. — are routed through Phase 3's confidence gate.

### Default run (no live calls)

`./gradlew :translator:translate`

For each Medium-bucket goal:
1. Look up `translator/.cache/llm/<sha256>.json`.
2. **Cache hit** with `Ok` result: write the cached JS to
   `bedrock-out/<modId>/behavior_pack/scripts/goals/<GoalSimpleName>.ts`.
3. **Cache miss**: write a `// TODO LLM:` stub embedding the original Java in
   a comment block, so the file is valid TypeScript that a human can fill in.

No tokens spent. Determinism preserved.

### Enabling live LLM calls

```sh
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew :translator:translate --args="--with-llm"
```

For each Medium-bucket goal with no cache hit, the gate calls
`claude-opus-4-7` with:
- A 3-block cached system prompt (performance rules, vendored
  `@minecraft/server` type declarations, worked Java→Bedrock examples).
- A per-request user message containing the goal class source, resolved type
  info, owning entity summary, and mod manifest excerpt.

If the model self-rates confidence ≥ 0.8, the JS is cached and committed.
Below 0.8, it's demoted to a TODO stub and **not** cached (so a re-run with a
tweaked prompt is a clean attempt).

### Cost expectations

Per-run cost depends on how many cache misses you have. With a populated
cache, every Medium-bucket goal is a free local read. The first translation
of a goal at `claude-opus-4-7` costs roughly one prompt-cache-write of the
system blocks (~5–10K tokens) plus the per-goal user message (~1–3K tokens)
plus the response (~1–3K tokens).

See [Anthropic pricing](https://platform.claude.com/docs/en/pricing) for
current rates. Prompt caching means the system-prompt cost is amortized
across every Medium-bucket goal in the same session.

### Cache lifecycle

- Stored at `translator/.cache/llm/<sha256>.json`.
- The cache root is gitignored (see `.gitignore` `translator/.cache/`).
- Survives across runs unless `--clear-cache` is passed.
- Cache key is `sha256(java_source + prompt_version + model_id +
  bedrock_api_version)`. Bumping `TranslationPrompt.PROMPT_VERSION`,
  switching models, or pinning a new Bedrock scripting API version all bust
  every cached entry.

### Freezing a translation

The committed `bedrock-out/<modId>/behavior_pack/scripts/goals/*.ts` is the
source of truth. The cache is a build artifact. You can:

1. Run `--with-llm` once locally to populate the cache and write good JS.
2. Commit the resulting `bedrock-out/.../scripts/goals/<GoalClass>.ts`.
3. From then on, anyone who runs `:translate` (without `--with-llm`) gets the
   same JS via cache hit (or, if they ran `--clear-cache`, via a stub that
   their committed file already replaces).

If a future translation regresses, the committed JS in `bedrock-out/` reflects
the last known-good output.

## Phase 3 internals

```
translator/src/main/kotlin/com/tweeks/translator/java/llm/
    ClaudeClient.kt         sealed iface + MockClaudeClient
    RealClaudeClient.kt     anthropic-java SDK wrapper (not exercised in tests)
    Cache.kt                on-disk cache, per spec layout
    TranslationPrompt.kt    builds 3-block system prompt + per-request user msg
    ConfidenceGate.kt       cache lookup, model call, confidence threshold
```

The `RealClaudeClient` is wired but **not** invoked by tests — every test
uses `MockClaudeClient` exclusively. The hard separation makes "no live calls"
a build-time invariant.
