# Litra autonomous iteration policy

This project runs a recurring, guarded improvement loop.

## Cadence

- One tick every 2 hours. If the previous run or Codex session is still active, skip that tick.
- Every tick uses GPT twice: one implementation pass and one independent review pass. Fix anything the review finds before opening/merging the PR.
- One meaningful change per run. If there is nothing worth changing, do a read-only audit, update the backlog, and report "no change" rather than churning code.
- Keep at most one merged PR per tick. Do not create a second feature while the first is still in CI.

## Non-negotiable guardrails

- **Do not create a tag or GitHub Release.**
- **Do not bump `versionName` / `versionCode`.** Release decisions are human decisions.
- At most one PR per run. Merge only after CI is green.
- Do not rewrite or revert previous work unless it is a confirmed regression.
- No placeholder/fake features. Every merged feature must work end-to-end.
- Preserve the product rules in `AGENTS.md` and `docs/product-goal.md`.
- If OpenAI/Codex is unavailable, do not modify code or Git; report the blocker and stop.
- Do not change app permissions, backup format or database schema without a migration/test.
- Keep the app local-only unless a feature explicitly requires otherwise.

## Priority order

1. **P0**: crash, data loss, broken import/reader, regression of 99%/FINISHED rules.
2. **P1**: UI polish, accessibility, reader ergonomics, performance, test gaps.
3. **P2**: high-value features (offline backup robustness, metadata editing, stats, export).
4. **P3**: refactors and developer experience.

## Backlog candidates

- Reader: edge tap zones / page-turn behavior without breaking selection; center-tap zone polish.
- Reader: brightness gesture, orientation lock, font family/typography refinements.
- Library: bulk selection, metadata edit, better empty/loading states.
- Backup: encryption option, restore preview/conflict UI, partial restore.
- Accessibility: TalkBack/manual multi-device pass, larger tap targets, contrast.
- Performance: startup, large library (500+ books), cover thumbnails, baseline profile.
- Release: after human approval, one `v0.2.0` release; no intermediate versions.

## Acceptance ownership

The task director (the agent reading this policy) only:

1. defines one concrete, user-visible acceptance goal;
2. dispatches it to GPT/Codex;
3. tracks the result and reports it.

GPT owns implementation **and acceptance**:

- run unit/lint/R8/connected tests as appropriate;
- inspect the diff for placeholders/TODO/unrelated rewrites;
- install the APK and capture/inspect real screenshots for UI work;
- open a real EPUB for reader work;
- verify the existing product rules;
- use a second GPT/Codex pass for independent review if useful;
- merge only after its own acceptance evidence is green;
- append the result to `docs/RUN_LOG.md` with PR, commit, test evidence and screenshots.

The task director must not re-run the acceptance suite or duplicate the review. If GPT's report lacks evidence, send it back; do not verify it personally.

## Each run

1. Inspect `git status`, `main`, open issues/PRs, latest Release, CI.
2. Pick the highest-priority item that can be completed end-to-end.
3. Define a concrete acceptance target and dispatch it to GPT/Codex; use `gpt-6-luna` if `gpt-6.1-sol` is at capacity.
4. GPT implements, tests, accepts, commits, opens PR, waits for CI, merges.
5. GPT updates `docs/RUN_LOG.md`.
6. Report GPT's outcome/evidence, PR, remaining risk and next candidate. Do not re-verify.

## Release policy

Only a human can approve a release. When approved, do exactly one release for the batch (e.g. `v0.2.0`), with release notes and APK; do not create milestone/nightly tags.
