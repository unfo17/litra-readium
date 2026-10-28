# Autonomous run log

Every autonomous tick must leave a verifiable outcome. GPT owns implementation, acceptance and this log; the task director only dispatches and reports.

## Format

| Date | Goal / acceptance | Outcome | Evidence | PR / commit | Status |
|---|---|---|---|---|---|
| 2026-10-28 | Reader chrome, external open, backup/restore, home empty modules | UI + 4 features shipped | 15 unit / 10 device / lint / R8 / screenshots | #9 / `23dcd5b` | merged; unreleased |

## Rules

- A row is added by GPT after its own acceptance.
- "Audit only" is allowed but labelled `no-output`; it does not count as a delivered improvement.
- A red CI, missing screenshot/manual check, or placeholder code is `rework` or `rejected`, never `merged`.
- Link the PR and commit; keep evidence concrete (test counts, emulator/version, screenshot paths).
- The task director does not independently re-verify; missing evidence means the task is sent back to GPT.
