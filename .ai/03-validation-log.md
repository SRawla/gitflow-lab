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

## Phase 2 — Feature 2 + QA Bug Fix (conflict test)

**Goal:** Validate that an open (unmerged) auto-qa-pr does NOT override a bug fix that lands on qa independently.

### Scenario Setup

- Feature 2 (`feat/user-course-assignment`) touches `CourseController.java` or `CourseService.java`
- Bug fix (`fix/course-logging`) also touches same file(s) on qa
- Feature 2 auto-qa-pr stays OPEN while bug fix merges to qa
- Expected: open PR gets conflict → forces manual resolution (no silent override)

### Actions

| # | Action | Type | Actor | Status | Notes |
|---|--------|------|-------|--------|-------|
| 1 | Create `feat/user-course-assignment` branch from develop | 👤 | Dev | ⏳ | |
| 2 | Implement user-course join table + endpoints | 👤 | Dev | ⏳ | Intentionally modifies CourseController/Service |
| 3 | PR `feat/user-course-assignment` → develop | 👤 | Dev | ⏳ | |
| 4 | `auto-qa-pr.yaml` fires → cherry-pick PR to qa | 🤖 | System | ⏳ | **DO NOT MERGE — leave open** |
| 5 | QA reports bug: missing logging on Course endpoints | 👤 | QE | ⏳ | |
| 6 | Create `fix/course-logging` branch from qa | 👤 | Dev | ⏳ | Adds @Slf4j + log statements to CourseController |
| 7 | PR `fix/course-logging` → qa | 👤 | Dev | ⏳ | |
| 8 | Merge fix PR to qa | 👤 | QE/Ops | ⏳ | |
| 9 | `sync-qa-to-develop.yaml` (GR1) fires → PR qa→develop | 🤖 | System | ⏳ | **TEST: does GR1 work?** |
| 10 | Check open Feature 2 auto-qa-pr for conflict | 👤 | Verify | ⏳ | **TEST: conflict or clean merge?** |
| 11 | Merge GR1 PR (qa→develop) | 👤 | Dev | ⏳ | Fix now on both qa + develop |
| 12 | Resolve Feature 2 auto-qa-pr conflict (if any) | 👤 | Dev | ⏳ | Manual resolution preserves fix |

### Expected Outcomes

| Test | Expected | Actual |
|------|----------|--------|
| GR1 auto-fires on qa push | PR created: qa → develop | |
| GR1 PR contains logging fix | Yes — same commits | |
| Feature 2 auto-qa-pr has conflict | Yes — same file modified differently | |
| Merging Feature 2 PR does NOT lose logging fix | Fix preserved after resolution | |

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

---

## Conclusions (fill after all phases)

| Use Case | Validated? | Manual Actions Required | Notes |
|----------|-----------|------------------------|-------|
| Feature → develop → auto-PR to qa | | | |
| QA bug fix → qa → GR1 auto-sync to develop | | | |
| Open PR does not override fix (conflict forces resolution) | | | |
| Release cut (qa→master→tag→release branch) | | | |
| Build-on-tag auto-fires | | | |
| Forward-port auto-fires | | | |
| Deploy prod from tag | | | |
| Hotfix → patch tag → redeploy | | | |
