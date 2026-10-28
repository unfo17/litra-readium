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

## Each run

1. Inspect `git status`, `main`, open issues/PRs, latest Release, CI.
2. Pick the highest-priority item that can be completed end-to-end.
3. Give GPT/Codex a focused goal; use `gpt-6-luna` if `gpt-6.1-sol` is at capacity.
4. Implement, test (unit + lint + R8 + connected when possible), update docs.
5. Commit → PR → CI → merge.
6. Report: change, tests, PR, remaining risk, next candidate.

## Release policy

Only a human can approve a release. When approved, do exactly one release for the batch (e.g. `v0.2.0`), with release notes and APK; do not create milestone/nightly tags.
