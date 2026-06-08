# Workflow Fixes & Gaps — Task List

Identified during session 2026-06-08 via full workflow scan + end-to-end scenario analysis.
All fixes apply to `.github/workflows/` in `SRawla/gitflow-lab`.

---

## Background — Loop Prevention Design

Three workflows auto-create PRs. Each must skip commits that originated from another
automation to prevent recursive loops.

| Workflow | Trigger | Creates PR to | Must skip |
|---|---|---|---|
| `auto-qa-pr` | push to `develop` | `qa` | `[forward-port]` commits |
| `sync-qa-to-develop` GR1 | push to `qa` | `develop` | `[auto-promote]` AND `[forward-port]` commits |
| `forward-port` GR2 | tag push (`v*`) | `qa` AND `develop` | n/a (originator) |

### Commit prefix conventions (must be consistent)

| Prefix | Set by | Meaning |
|---|---|---|
| `[auto-promote]` | `auto-qa-pr` | Cherry-pick from develop to qa |
| `[forward-port]` | `sync-qa-to-develop` GR1 | Cherry-pick from qa to develop |
| `[forward-port]` | `forward-port` GR2 | Cherry-pick from release/* to qa/develop |

### Loop scenarios verified

| Scenario | Path | Protected by | Status |
|---|---|---|---|
| feat/fix → develop → qa → develop | `[auto-promote]` lands on qa → GR1 | GR1 skips `[auto-promote]` | ✅ |
| fix directly on qa → develop → qa | `[forward-port]` lands on develop → auto-qa-pr | auto-qa-pr skips `[forward-port]` | ✅ |
| hotfix → release tag → GR2 to qa AND develop | `[forward-port]` lands on qa → GR1 fires | GR1 does NOT skip `[forward-port]` | ❌ GAP → TASK-06 |

---

## End-to-End Flow Coverage

### Flow 1 — Feature → DEV
`feat/xyz` → develop → build → deploy-dev

| Step | Workflow | Active? | Task |
|---|---|---|---|
| PR validation + tests | `pr.yaml` | ✅ | — |
| Auto cherry-pick to qa | `auto-qa-pr.yaml` | ✅ | TASK-06 |
| Manual build | `build.yaml` | ✅ | — |
| Deploy to dev | `deploy-dev.yaml` | ✅ | TASK-01 |
| Smoke test after deploy | — | ❌ | TASK-09 |

### Flow 2 — QA Promotion → QA Deployment
auto-qa PR merged → build → deploy-qa

| Step | Workflow | Active? | Task |
|---|---|---|---|
| QA PR auto-created | `auto-qa-pr.yaml` | ✅ | TASK-06 |
| QA merge syncs to develop | `sync-qa-to-develop.yaml` | ✅ | TASK-06 |
| Manual build from qa | `build.yaml` | ✅ | — |
| Deploy to QA | `deploy-qa.yaml` | ✅ | TASK-02 |
| Smoke test after deploy | — | ❌ | TASK-09 |
| Stale qa-promote PR cleanup | `stale-qa-pr.yaml` | ❌ backup | TASK-11 |
| Re-promote after stale close | `re-promote.yaml` | ❌ backup | TASK-12 |

### Flow 3 — Release Cut → Prod Deployment
`release-pr` → qa→master → `release-tag` → tag → `build-on-tag` → `deploy-prod`

| Step | Workflow | Active? | Task |
|---|---|---|---|
| Open qa→master PR | `release-pr.yaml` | ✅ | — |
| PR validation | `pr.yaml` | ✅ | — |
| Cut `v{}.0` tag + fork release branch | `release-tag.yaml` | ✅ | TASK-08 |
| Auto-build on tag | `build-on-tag.yaml` | ✅ | TASK-07 |
| Forward-port fires on `.0` tag | `forward-port.yaml` | ✅ | TASK-04, TASK-05 |
| Deploy prod | `deploy-prod.yaml` | ✅ | TASK-03 |
| Smoke test after deploy | — | ❌ | TASK-09 |
| Drift detection | `drift-check.yaml` | ❌ backup | TASK-10 |

### Flow 4 — Hotfix/Patch → Prod
`hotfix/*` → `release/X` → `patch-tag` → tag → `build-on-tag` → `forward-port` → `deploy-prod`

| Step | Workflow | Active? | Task |
|---|---|---|---|
| PR hotfix → release/* | `pr.yaml` | ✅ | — |
| Cut next patch tag | `patch-tag.yaml` | ✅ | TASK-13 |
| Auto-build on tag | `build-on-tag.yaml` | ✅ | TASK-07 |
| Cherry-pick to qa + develop | `forward-port.yaml` | ✅ | TASK-04, TASK-05 |
| GR1 fires on qa push from forward-port | `sync-qa-to-develop.yaml` | ✅ | TASK-06 |
| Deploy prod | `deploy-prod.yaml` | ✅ | TASK-03 |
| Smoke test after deploy | — | ❌ | TASK-09 |

### Flow 5 — QA Bug Fix
`fix/xyz` → qa (or develop) → GR1 auto-sync

| Step | Workflow | Active? | Task |
|---|---|---|---|
| PR fix → qa | `pr.yaml` | ✅ | — |
| PR fix → develop | `pr.yaml` | ✅ | — |
| GR1 cherry-picks qa fix to develop | `sync-qa-to-develop.yaml` | ✅ | TASK-06 |

### Guard Rails

| Guard Rail | Strategy Requirement | Active? | Task |
|---|---|---|---|
| GR1 — sync qa→develop | Cherry-pick on qa push | ✅ | TASK-06 |
| GR2 — forward-port on tag | Cherry-pick to qa+develop | ✅ | TASK-04, TASK-05 |
| GR3 — drift check | Daily cron, open GitHub issue | ❌ backup | TASK-10 |
| GR4 — branch protection | Direct push blocked | ✅ GitHub setting | — |

---

## Task List

### TASK-01 — `deploy-dev.yaml`: Remove rollout restart before Helm
**Priority:** High
**File:** `.github/workflows/deploy-dev.yaml`

**Problem:**
`kubectl rollout restart deployment/dev` runs BEFORE `helm upgrade`.
Restarts pods on the old image before the new one is applied.
On first deploy the deployment does not exist yet — fails silently via `|| true`.
`helm upgrade --wait` already handles readiness.

**Fix:** Remove the `kubectl rollout restart` line entirely.

**Status:** ✅ Done

---

### TASK-02 — `deploy-qa.yaml`: Remove rollout restart before Helm
**Priority:** High
**File:** `.github/workflows/deploy-qa.yaml`

**Problem:** Same as TASK-01.

**Fix:** Remove the `kubectl rollout restart` line entirely.

**Status:** ✅ Done

---

### TASK-03 — `deploy-prod.yaml`: Three fixes
**Priority:** High
**File:** `.github/workflows/deploy-prod.yaml`

#### 3a — Checkout at tag, not master
**Problem:** Workflow checks out `master` HEAD to get the Helm chart.
If master has moved forward since the release tag was cut, the chart files
deployed to prod do not match what was built and tested for that tag.

**Fix:** Change `ref: master` to `ref: ${{ inputs.image_tag }}`.

#### 3b — Validate tag format before deploy
**Problem:** No validation on `image_tag` input. A developer could accidentally
input `develop.5.abc1234` or `qa.8.xyz` and deploy a non-release build to prod.

**Fix:** Add validation step:
```bash
TAG="${{ inputs.image_tag }}"
if [[ ! "$TAG" =~ ^v[0-9]{4}\.[0-9]{2}\.[0-9]+$ ]]; then
  echo "::error::image_tag must be a release tag (e.g. v2026.06.1). Got: $TAG"
  exit 1
fi
```

#### 3c — Remove rollout restart before Helm
**Problem:** Same as TASK-01 / TASK-02.

**Fix:** Remove the `kubectl rollout restart` line entirely.

**Status:** ✅ Done

---

### TASK-04 — `forward-port.yaml`: Skip `.0` tags
**Priority:** High
**File:** `.github/workflows/forward-port.yaml`

**Problem:**
`release-tag.yaml` pushes the `v{VERSION}.0` tag FIRST, then creates
`release/{VERSION}` branch. The tag push immediately triggers `forward-port.yaml`
before the release branch exists — causing it to fail with "branch not found".
Additionally, a `.0` tag is the initial release point with no patch commits yet.
There is nothing to forward-port until `.1`, `.2`, etc.

**Fix:** Add early exit:
```bash
TAG="${{ github.ref_name }}"
if [[ "$TAG" =~ \.0$ ]]; then
  echo "Skipping forward-port for initial release tag $TAG (no patches yet, release branch just forked)"
  exit 0
fi
```

**Status:** ✅ Done

---

### TASK-05 — `forward-port.yaml`: Cherry-pick instead of merge
**Priority:** High
**File:** `.github/workflows/forward-port.yaml`

**Problem:**
Currently does `git merge --no-ff origin/${RELEASE_BRANCH}` which brings the
ENTIRE release branch history into qa/develop. If the release branch has diverged,
this creates a massive PR with unrelated history and high conflict risk.
Only the patch-specific commits should flow forward.

**Fix:** Cherry-pick only commits that are on `release/X` but NOT on the target:
```bash
COMMITS=$(git log --no-merges --format="%H" \
  origin/${TARGET}..origin/${RELEASE_BRANCH} | tac)

if [ -z "$COMMITS" ]; then
  echo "No new commits to forward-port to $TARGET"
  continue
fi

for SHA in $COMMITS; do
  if ! git cherry-pick "$SHA" --allow-empty; then
    CONFLICT=true
    git cherry-pick --abort
    break
  fi
done
```
On conflict: commit empty placeholder, open draft PR.
Developer is responsible for resolving.

**Status:** ✅ Done

---

### TASK-06 — `sync-qa-to-develop.yaml`: Add `[forward-port]` skip
**Priority:** High
**File:** `.github/workflows/sync-qa-to-develop.yaml`

**Problem:**
GR1 currently only skips `[auto-promote]` commits.
When GR2 (`forward-port.yaml`) merges a patch from `release/*` into qa,
the commit message contains `[forward-port]`. GR1 fires on that qa push
and opens ANOTHER develop PR — a duplicate, since GR2 already opened one.

**Fix:** Extend skip condition:
```bash
if [[ "$COMMIT_MSG" == *"[auto-promote]"* ]] || [[ "$COMMIT_MSG" == *"[forward-port]"* ]]; then
  echo "Skipping: already handled by automation, no sync needed"
  exit 0
fi
```

**Status:** ✅ Done

---

### TASK-07 — `build-on-tag.yaml`: Run tests before Docker build
**Priority:** Critical
**File:** `.github/workflows/build-on-tag.yaml`

**Problem:**
Currently runs `./mvnw -B -DskipTests clean package` — tests are skipped.
This means a hotfix or release with a failing test will still build, load into kind,
and become deployable to prod. There is no test gate on the production artifact path.

Note: `pr.yaml` runs tests on the PR, but if a direct tag is cut without a clean
PR (e.g., emergency hotfix bypassing normal flow), tests would be skipped entirely.

**Fix:** Replace `DskipTests` with full verify:
```bash
./mvnw -B verify
```
If tests fail, the build step fails and no Docker image is produced.

**Status:** ✅ Done

---

### TASK-08 — `release-tag.yaml`: Validate master HEAD == qa HEAD before tagging
**Priority:** Critical
**File:** `.github/workflows/release-tag.yaml`

**Problem:**
If someone runs `release-tag.yaml` without first completing the qa→master merge
(via `release-pr.yaml`), the tag is cut from a stale master that does not contain
the latest qa code. The prod build would then be missing features that are in qa.

**Fix:** Add validation step after checkout:
```bash
git fetch origin master qa

MASTER_SHA=$(git rev-parse origin/master)
QA_SHA=$(git rev-parse origin/qa)

if [[ "$MASTER_SHA" != "$QA_SHA" ]]; then
  echo "::error::master ($MASTER_SHA) is not at the same commit as qa ($QA_SHA)."
  echo "::error::Merge qa into master first using release-pr.yaml before cutting the tag."
  exit 1
fi
echo "master == qa at $MASTER_SHA — safe to tag"
```

**Status:** ✅ Done

---

### TASK-09 — `deploy-dev/qa/prod.yaml`: Add smoke test after deploy
**Priority:** Medium
**Files:** `.github/workflows/deploy-dev.yaml`, `deploy-qa.yaml`, `deploy-prod.yaml`

**Problem:**
After `helm upgrade --wait` and `kubectl rollout status`, the workflow reports
success based on pod readiness only. A pod can be running but the app can still
be unhealthy (failed DB connection, bad config, startup exception).
There is no confirmation that the app actually responds to requests.

**Fix:** Add a smoke test step after the verify step in each deploy workflow:

For dev/qa (internal hostname):
```bash
# Wait for ingress to propagate then hit health endpoint
sleep 5
curl -sf --retry 5 --retry-delay 3 \
  -H "Host: dev.tbs.local" \
  http://localhost:8080/actuator/health \
  | grep -q '"status":"UP"' \
  && echo "Smoke test passed" \
  || { echo "::error::Smoke test failed — app not healthy after deploy"; exit 1; }
```
Replace `dev.tbs.local` with `qa.tbs.local` or `prod.tbs.local` per workflow.

**Status:** ✅ Done

---

### TASK-10 — Activate `drift-check.yaml` (GR3)
**Priority:** High
**Action:** Move `.github/workflows-backup/drift-check.yaml` → `.github/workflows/drift-check.yaml`

**Problem:**
GR3 (drift detector) is required by the branching strategy. It runs daily and opens
a GitHub Issue if `develop` has fallen behind `qa`. Currently in backup — not running.
Silent drift means branches can diverge without anyone noticing until a merge conflict.

**No code changes needed** — the existing file is correct:
- Daily cron at 09:00 UTC
- Opens issue with `drift-alert` label if drift detected
- Auto-closes the issue when drift is resolved

**Status:** ✅ Done

---

### TASK-11 — Activate `stale-qa-pr.yaml`
**Priority:** Medium
**Action:** Move `.github/workflows-backup/stale-qa-pr.yaml` → `.github/workflows/stale-qa-pr.yaml`

**Problem:**
Without this, `qa-promote/*` PRs accumulate indefinitely. Over time ops loses track
of which PRs are genuinely pending vs abandoned. The 30-day auto-close enforces
timely review and keeps the PR list clean.

**No code changes needed** — the existing file is correct:
- Daily cron at 10:00 UTC
- Finds open `qa-promote/*` PRs older than 30 days
- Closes with comment explaining how to re-promote if still needed

**Status:** ✅ Done

---

### TASK-12 — Activate `re-promote.yaml`
**Priority:** Medium
**Action:** Move `.github/workflows-backup/re-promote.yaml` → `.github/workflows/re-promote.yaml`

**Problem:**
After `stale-qa-pr.yaml` auto-closes a PR, or after ops closes a PR that turns out
to still be needed, there is no automated way to re-cherry-pick that commit to qa.
Without this workflow, the developer has to do it manually — the exact problem the
strategy is designed to avoid.

**No code changes needed** — the existing file is correct:
- Manual dispatch, input = commit SHA
- Cherry-picks that specific commit from develop onto a fresh qa-promote branch
- Opens PR to qa (same pattern as auto-qa-pr)

**Status:** ✅ Done

---

### TASK-13 — `patch-tag.yaml`: Validate release branch has new commits since last tag
**Priority:** Medium
**File:** `.github/workflows/patch-tag.yaml`

**Problem:**
Nothing prevents running `patch-tag.yaml` twice in a row on a release branch
that has not received any new commits since the last tag. This would cut an
identical tag (same code, new tag number) — a meaningless release.

**Fix:** Add a check that the release branch HEAD is ahead of the latest tag:
```bash
LATEST_TAG_SHA=$(git rev-list -n 1 "$LATEST")
BRANCH_SHA=$(git rev-parse HEAD)

if [[ "$LATEST_TAG_SHA" == "$BRANCH_SHA" ]]; then
  echo "::error::Release branch has no new commits since $LATEST. Merge a patch/hotfix first."
  exit 1
fi
echo "Branch is ahead of $LATEST — safe to tag"
```

**Status:** ✅ Done

---

---

### TASK-14 — Remove `actions/setup-java@v4` from build and PR workflows
**Priority:** Medium — performance
**Files:** `.github/workflows/build.yaml`, `build-on-tag.yaml`, `pr.yaml`

**Problem:**
`actions/setup-java@v4` downloads and configures the JDK on every workflow run.
On a persistent self-hosted Windows runner, Java 21 is permanently installed
machine-wide. This action adds unnecessary overhead (download + PATH config)
on every single build and PR check — even when nothing has changed.

**Fix:** Replace the `actions/setup-java@v4` step with a lightweight version check:
```bash
JAVA_VER=$(java -version 2>&1 | head -1)
if ! java -version 2>&1 | grep -q '"21'; then
  echo "::error::Java 21 required but found: $JAVA_VER"
  exit 1
fi
echo "✅ Java 21 available (machine-installed, no download needed)"
```
If Java is ever missing from the runner, the step fails fast with a clear error.

**Note on Maven deps:** `~/.m2/repository` on self-hosted runner persists between
runs on the same machine. No `actions/cache` needed — dependencies are already local.

**Status:** ✅ Done

---

### TASK-15 — `build-on-tag.yaml`: Remove redundant second Maven compile
**Priority:** Medium — performance
**File:** `.github/workflows/build-on-tag.yaml`

**Problem:**
The workflow currently runs two Maven steps:
1. `./mvnw -B verify` — compile → test → package → verify (produces JAR in `target/`)
2. `./mvnw -B -DskipTests clean package` — deletes `target/`, recompiles from scratch

Step 2 throws away all the work from step 1 and recompiles unnecessarily.
The Maven `verify` lifecycle already includes `package`, so the JAR is already
in `target/` after step 1. The Dockerfile just copies `target/*.jar`.

**Fix:** Remove the second `./mvnw -B -DskipTests clean package` step entirely.
Rename step 1 to `Run tests and build JAR` to make the intent clear.

**Status:** ✅ Done

---

### TASK-16 — Enable Docker BuildKit in build workflows
**Priority:** Low — performance
**Files:** `.github/workflows/build.yaml`, `build-on-tag.yaml`

**Problem:**
Docker builds run without BuildKit. BuildKit provides smarter layer caching,
parallel step execution, and better cache reuse across runs. On a self-hosted runner
the Docker layer cache already persists on disk — BuildKit makes better use of it.

**Fix:** Add `DOCKER_BUILDKIT: "1"` env var to the docker build step:
```yaml
- name: Build Docker image
  env:
    DOCKER_BUILDKIT: "1"
  run: docker build -t ...
```

**Status:** ✅ Done

---

## Execution Order

Apply in this sequence to avoid dependency issues:

| Order | Task | Reason | Status |
|---|---|---|---|
| 1 | TASK-06 | Close GR1 loop gap first — affects all flows | ✅ Done |
| 2 | TASK-07 | Critical prod safety — run before any release testing | ✅ Done |
| 3 | TASK-08 | Critical release guard — run before Phase 3 validation | ✅ Done |
| 4 | TASK-04 + TASK-05 | Forward-port correctness — needed for hotfix flow | ✅ Done |
| 5 | TASK-01 + TASK-02 + TASK-03 | Deploy fixes — independent of above | ✅ Done |
| 6 | TASK-13 | Patch tag guard — before hotfix flow testing | ✅ Done |
| 7 | TASK-09 | Smoke tests — add after deploys are stable | ✅ Done |
| 8 | TASK-10 | Activate GR3 — after main flows are validated | ✅ Done |
| 9 | TASK-11 + TASK-12 | Activate stale/re-promote — housekeeping | ✅ Done |
| 10 | TASK-14 | Remove actions/setup-java download on self-hosted runner  | ✅ Done |
| 11 | TASK-15 | Remove redundant Maven recompile in build-on-tag  | ✅ Done |
| 12 | TASK-16 | Enable Docker BuildKit for better layer cache reuse  | ✅ Done |

---

## Validation Checklist (run after all tasks applied)

| Scenario | Expected Outcome |
|---|---|
| Merge feature to develop | auto-qa-pr creates qa PR; GR1 skips on qa merge |
| Merge fix directly to qa | GR1 opens develop PR; auto-qa-pr skips on develop merge |
| Cut patch tag `v2026.06.1` | build-on-tag runs tests then builds; GR2 cherry-picks to qa + develop; GR1 skips GR2 commits |
| Cut release tag `v2026.07.0` | Blocked if master ≠ qa; build-on-tag runs tests; GR2 skips (`.0` tag) |
| Deploy prod with `develop.5.abc` | deploy-prod rejects with error |
| Deploy prod with `v2026.06.1` | deploy-prod checks out tag; deploys; smoke test passes |
| Run patch-tag with no new commits | Blocked with error |
| Drift develops (simulate) | GR3 opens GitHub issue next cron run |
| qa-promote PR open 30+ days | stale-qa-pr auto-closes with comment |
| Re-promote closed PR | re-promote opens fresh qa PR for that commit SHA |
