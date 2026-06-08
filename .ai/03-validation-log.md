# Validation Log — Branching Strategy v2

Tracks every action (manual + automated) during end-to-end validation of the branching strategy.

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Completed successfully |
| ⏳ | In progress / waiting |
| ❌ | Failed (see notes) |
| 🤖 | Auto-triggered by workflow |
| 👤 | Manual action (human) |

---

## Phase 1 — Feature 1 (Course CRUD) → develop + qa

| # | Action | Type | Actor | Status | Notes |
|---|--------|------|-------|--------|-------|
| 1 | Create `feat/course-crud` branch from develop | 👤 | Dev | ✅ | Course entity, controller, service, repo, DTOs, migrations V4+V5 |
| 2 | PR `feat/course-crud` → develop | 👤 | Dev | ✅ | Merged |
| 3 | `auto-qa-pr.yaml` fires → cherry-pick PR to qa | 🤖 | System | ✅ | PR auto-created |
| 4 | Merge auto-qa PR to qa | 👤 | Ops | ✅ | Merged |
| 5 | Build from develop (manual dispatch) | 👤 | Dev | ✅ | Build #11 |
| 6 | Build from qa (manual dispatch) | 👤 | Dev | ✅ | Build #12 |
| 7 | Deploy to tbs-dev | 👤 | Dev | ✅ | Pod running, all endpoints verified |
| 8 | Deploy to tbs-qa | 👤 | Ops | ✅ | Pod running, all endpoints verified |

**Endpoints verified (both envs):** `/actuator/health` ✅ · `/info` ✅ · `/locations` (5 records) ✅ · `/courses` (4 records) ✅

---

## Phase 2 — Feature 2 + QA Bug Fixes (conflict test)

**Goal:** Validate GR1 (sync-qa-to-develop) in both happy path (clean cherry-pick) and conflict path, plus verify that fixes on qa propagate correctly to develop.

### Phase 2a — Feature 2 + Bug Fix 1 (Logging)

| # | Action | Type | Actor | Status | Notes |
|---|--------|------|-------|--------|-------|
| 1 | Create `feat/user-course-assignment` from develop | 👤 | Dev | ✅ | Enrollment entity, service, repo, DTO, V6 migration |
| 2 | PR → develop, merged | 👤 | Dev | ✅ | Squash merged |
| 3 | `auto-qa-pr.yaml` fires → PR #16 to qa | 🤖 | System | ✅ | CONFLICT on CourseController — left open intentionally |
| 4 | Create `fix/course-logging` from qa | 👤 | Dev | ✅ | Added @Slf4j + log lines to CourseController |
| 5 | PR `fix/course-logging` → qa, merged | 👤 | Dev | ✅ | PR #19 |
| 6 | GR1 fires → conflict detected | 🤖 | System | ✅ | CourseController differs (qa: logging only, develop: logging + enrollment) |
| 7 | GR1 opened conflict draft PRs #20, #21 | 🤖 | System | ✅ | Empty commit approach — 0 files changed |
| 8 | Conflict PRs closed without resolution | 👤 | Dev | ❌ | **MISTAKE** — fix was LOST on develop |
| 9 | Manual merge `origin/qa` into develop | 👤 | Dev | ✅ | Resolved conflict, brought logging + enrollment together |
| 10 | Build + deploy develop (tbs-dev) | 👤 | Dev | ✅ | All endpoints verified |
| 11 | Build + deploy qa (tbs-qa) | 👤 | Dev | ✅ | All endpoints verified |

### Phase 2b — Bug Fix 2 (UUID Error Handling + Count Endpoint)

| # | Action | Type | Actor | Status | Notes |
|---|--------|------|-------|--------|-------|
| 1 | Create `fix/uuid-error-handling` from qa | 👤 | Dev | ✅ | |
| 2 | Add GlobalExceptionHandler (new file) + /courses/count endpoint | 👤 | Dev | ✅ | Bug 2a: new file (clean), Bug 2b: modifies CourseController (conflict) |
| 3 | PR `fix/uuid-error-handling` → qa, merged | 👤 | Dev | ✅ | PR #22 |
| 4 | GR1 fires → conflict detected | 🤖 | System | ✅ | Opened draft PR #23 (0 files — empty commit approach) |
| 5 | Manual cherry-pick `51c29a2` onto develop | 👤 | Dev | ✅ | Resolved CourseController (kept enrollment + count) |
| 6 | Closed PR #23 | 👤 | Dev | ✅ | Resolved manually |
| 7 | Build + deploy both envs | 👤 | Dev | ⏳ | |

### Observations from Phase 2

| Test | Expected | Actual |
|------|----------|--------|
| GR1 auto-fires on qa push | PR created | ✅ Fires correctly |
| GR1 handles clean cherry-pick | Auto-merge to develop | ❓ Not yet tested (Bug 2a was part of same commit as Bug 2b) |
| GR1 handles conflict | Opens draft conflict PR | ✅ But PR has 0 files (empty commit approach) |
| Manual resolution preserves all code | Both branches have correct content | ✅ After manual merge/cherry-pick |

**Key learning:** GR1 conflict PRs with 0 files changed are a signal only — actual resolution requires manual `git merge` or `git cherry-pick` locally. Rule: NEVER close these without resolving first.

---

## Phase 3 — Release Cut (future)

| # | Action | Type | Actor | Status | Notes |
|---|--------|------|-------|--------|-------|
| 1 | `release-pr.yaml` dispatch → opens PR qa→master | 👤 | Tech Lead | ⏳ | |
| 2 | Review + merge qa→master PR | 👤 | Tech Lead | ⏳ | |
| 3 | `release-tag.yaml` dispatch (input: YYYY.MM) | 👤 | Tech Lead | ⏳ | Cuts vYYYY.MM.0 + forks release/YYYY.MM |
| 4 | `build-on-tag.yaml` auto-fires | 🤖 | System | ⏳ | Builds image tagged vYYYY.MM.0 |
| 5 | `forward-port.yaml` auto-fires | 🤖 | System | ⏳ | PRs to qa + develop |
| 6 | `deploy-prod.yaml` dispatch (input: vYYYY.MM.0) | 👤 | Ops | ⏳ | Deploy to tbs-prod |
| 7 | Verify prod endpoints | 👤 | Ops | ⏳ | |

---

## Phase 4 — Hotfix / Patch (future)

| # | Action | Type | Actor | Status | Notes |
|---|--------|------|-------|--------|-------|
| 1 | Create `hotfix/TICKET` from release/YYYY.MM | 👤 | Dev | ⏳ | |
| 2 | PR hotfix → release/YYYY.MM | 👤 | Dev | ⏳ | |
| 3 | Merge hotfix PR | 👤 | Tech Lead | ⏳ | |
| 4 | `patch-tag.yaml` dispatch | 👤 | Tech Lead | ⏳ | Auto-increments to vYYYY.MM.1 |
| 5 | `build-on-tag.yaml` auto-fires | 🤖 | System | ⏳ | |
| 6 | `forward-port.yaml` auto-fires | 🤖 | System | ⏳ | PRs to qa + develop |
| 7 | `deploy-prod.yaml` dispatch (vYYYY.MM.1) | 👤 | Ops | ⏳ | |

---

## Merge Strategy Rules

| Target Branch | Allowed Merge Type | Reason |
|---------------|-------------------|--------|
| develop | **Squash & Merge** | Clean single commit per feature/fix; linear history on develop |
| qa | **Squash & Merge** | Same — one commit per cherry-pick/fix; keeps qa history readable |
| master | **Merge commit** (no squash) | Preserves full traceability from qa; fast-forward when possible |
| release/* | **Squash & Merge** | One commit per hotfix/patch; clean release branch |

**Key rules:**
- Squash & merge is the default for all PRs into develop, qa, and release/*
- PR title becomes the squash commit message — must be clean and descriptive
- No merge commits on develop/qa/release/* (keeps `git log --oneline` useful)
- master allows merge commits because it's a promotion target (qa→master), not a dev target

---

## Observations & Issues

| Date | Observation |
|------|-------------|
| 2026-06-08 | Build #11 (develop) 8m46s, #12 (qa) 15m18s — Dockerfile was doing redundant Maven build. Fixed to single-stage. |
| 2026-06-08 | forward-port + sync-qa-to-develop showed info runs on branch pushes but skipped correctly (GitHub Actions quirk). |
| 2026-06-08 | GR1 workflow must exist on the TARGET branch (qa has push trigger). Fixed by copying all workflows to qa. |
| 2026-06-08 | YAML syntax errors: unicode arrows (`→`) + inline multi-line `--body` break GitHub Actions. Fixed with bash variables. |
| 2026-06-08 | GR1 cherry-picks only the single commit from `github.sha` — no memory of past failures. Lost fix by closing conflict PR. |
| 2026-06-08 | `pr.yaml` branch validation was missing `fix/* → qa` rule. Added to allow QA bug fixes to PR directly into qa. |
| 2026-06-08 | `setup-java` with `cache: maven` downloads ~3GB cache archive every run on self-hosted runner. Removed — `.m2` persists on disk. |

---

## Conclusions (fill after all phases)

| Use Case | Validated? | Manual Actions Required | Notes |
|----------|-----------|------------------------|-------|
| Feature → develop → auto-PR to qa | ✅ | Merge auto-qa PR (or resolve conflict) | PR #13 (clean), #16 (conflict) |
| QA bug fix → qa → GR1 auto-sync to develop | ✅ | Resolve conflict PR if cherry-pick fails | GR1 fires but 0-file conflict PRs need manual merge |
| Open PR does not override fix (conflict forces resolution) | ✅ | Manual conflict resolution | PR #16 showed conflict correctly |
| Release cut (qa→master→tag→release branch) | ⏳ | | Phase 3 |
| Build-on-tag auto-fires | ⏳ | | Phase 3 |
| Forward-port auto-fires | ⏳ | | Phase 3 |
| Deploy prod from tag | ⏳ | | Phase 3 |
| Hotfix → patch tag → redeploy | ⏳ | | Phase 4 |
