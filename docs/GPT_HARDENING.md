# Post-release hardening — no new release

You are continuing Litra after `v0.1.0`. This pass is **quality hardening**, not a redesign.

Read:
1. `AGENTS.md`
2. `docs/product-goal.md`
3. `README.md`
4. Current source and existing tests

## Hard constraints

- **Do not create a tag or GitHub Release.**
- **Do not bump the version.**
- Do not change the product behavior or visual direction.
- One branch: `fix/post-release-hardening`; one PR; merge only after CI passes.
- Keep `v0.1.0` as the latest release.

## Work items

### 1. Privacy / manifest

- Remove `android:usesCleartextTraffic="true"`.
- Remove the `INTERNET` permission **if** no shipped feature requires it. `ReadiumService` uses `DefaultHttpClient`, but this app is a local reader; if removing INTERNET breaks only remote-linked resources, document that as intentional. Otherwise justify keeping it in the PR.
- Keep `allowBackup="false"` unless you add a real backup feature.

### 2. Detail metadata tags

- Book detail currently shows the kind + custom categories only.
- Add real metadata tags alongside them, using actual data:
  - language (e.g. `en`, `中文`)
  - format (EPUB)
  - file size, formatted
- Do not invent genres. Keep the rectangular content cards.
- Avoid duplicate tags (e.g. `图书` appearing twice).

### 3. Accessibility and semantics

- Add `onClickLabel` / content descriptions for:
  - horizontal book cards (open reader / open detail)
  - library grid and list rows (state what click does)
  - want-to-read covers
  - year-read covers
  - "标记读完" action
- Ensure "已读完" is exposed as text, not only color.
- Keep existing content descriptions for covers and FABs.
- Add semantics tests where practical.

### 4. Lifecycle-aware UI state

- Replace direct `collectAsState()` usage in Compose screens with `collectAsStateWithLifecycle()` where appropriate.
- Add `androidx.lifecycle:lifecycle-runtime-compose` if missing.
- Do not change ViewModel ownership.

### 5. Tests and docs

- Add/update unit or instrumented tests for:
  - metadata tag formatting
  - the 99% / FINISHED state still not regressing
  - accessibility labels for a book card
- Update `README.md` and `docs/STATUS.md` with this hardening pass.
- Keep existing CI green.

## Verification

Run the project's normal build and tests, including `connectedDebugAndroidTest` if the emulator is available. If local Maven TLS blocks the build, use the existing `tools/build_with_curl.py`, but do not change CI.

## Deliverable

- PR URL
- commit SHA
- build/test/lint status
- what changed
- what remains risky
- explicit confirmation: no new tag, no new Release, version unchanged
