# Part 1 — Branching Strategy Demo & Practical Context

## Purpose of this repo

To **practically validate** the Branching Strategy v2 (Model C) end-to-end on a tiny app, before rolling it out to the real services. Every concept in the strategy — branches, tags, fork, promotion, forward-port, guard rails, workflow automation — must be observable here through actual git operations and GitHub Actions runs.

> **Reference doc for the strategy itself:** See the README refcard at the project root. This file is the branching strategy context. Which has info on current pain points, the new strategy, and the rationale behind it. The purpose of this sandbox repo is to validate that the strategy is workable in practice, and to refine the implementation details (branch naming, workflow triggers, etc.) before applying it to a real codebase.

---

## 1. Branches in this repo

Same structure as the real strategy. Four long-lived + temporary working branches.

| Branch | Role | Protection (in this sandbox) |
|---|---|---|
| `master` | Latest stable release pointer; fast-forwarded at each major release | PR required, self-approval allowed, only fast-forward from qa |
| `qa` | Release candidate; tested here before promotion | PR required, self-approval allowed, CI green |
| `develop` | In-flight integration of features, fixes, experiments | PR required, self-approval allowed, CI green |
| `release/YYYY.MM` | Long-lived per-release-line patch home (forked at each major release) | PR required, self-approval allowed, CI green |
| `feat/TICKET-desc` | Temporary feature branch, off `develop` | none |
| `fix/TICKET-desc` | Temporary bug branch, off `develop` or `qa` | none |
| `chore/TICKET-desc` | Temporary tech-debt branch, off `develop` | none |
| `patch/TICKET-desc` | Temporary patch branch, off a `release/YYYY.MM` | none |
| `hotfix/TICKET-desc` | Temporary emergency branch, off a `release/YYYY.MM` | none |

### Single-reviewer simplification

In the real strategy, `qa` and prod-promote PRs require 2 approvals (typically a Dev + Tech Lead, or Tech Lead + QE). In this sandbox **the owner self-approves**. Branch protection rules will be configured with `required_approving_review_count: 1` and `dismiss_stale_reviews: false`. This makes it a deliberate, documented deviation — not an oversight.

---

## 2. Tags

| Tag pattern | Cut from | When |
|---|---|---|
| `vYYYY.MM.0` | `master` | Major release (after qa → master fast-forward) |
| `vYYYY.MM.N` (N ≥ 1) | `release/YYYY.MM` | After each patch PR merges into the release branch |

Tags are immutable. Customer envs deploy tags, not branches.

---

## 3. Environments (simulated locally)

Three namespaces inside one local `kind` cluster, all sharing the same code paths, distinguished by hostname routing via `/etc/hosts`.

| Env | Namespace | Hostname | Pinned to |
|---|---|---|---|
| `dev` | `tbs-dev` | `dev.tbs.local` | `develop` branch (auto-rebuilt on push) |
| `qa` | `tbs-qa` | `qa.tbs.local` | `qa` branch (auto-rebuilt on push) |
| `prod` | `tbs-prod` | `prod.tbs.local` | A chosen tag (`vYYYY.MM.N`) via manual promote |

Hostnames route via the cluster's nginx-ingress to the right namespace. Local DNS via `/etc/hosts`:

```
127.0.0.1   dev.tbs.local qa.tbs.local prod.tbs.local
```

> **`tbs-`** prefix = test-branching-strategy. Keeps namespaces visually distinct in `kubectl get ns`.

---

## 4. Workflows to implement (and validate)

All triggered by GitHub Actions, executed on a **self-hosted runner** on this laptop so the workflow steps can reach the local `kind` cluster + local Docker + local files.

| Workflow file | Trigger | What it validates |
|---|---|---|
| `pr.yaml` | On PR | Branch-name regex + Maven verify + tests |
| `build-and-push.yaml` | Push to `develop` / `qa` / `release/**` | Builds image, tags `{branchtype}.{run}.{sha}`, pushes to ghcr.io, deploys to the matching namespace via Helm |
| `promote.yaml` | Manual dispatch | Promotes a chosen tag → chosen target env (writes to env values file, opens PR) |
| `release-tag.yaml` | Manual dispatch OR after prod deploy | Cuts the next `vYYYY.MM.N` from `release/YYYY.MM` and pushes |
| `forward-port.yaml` | Push of a tag matching `v[0-9]{4}.[0-9]{2}.*` | Opens auto-PRs `release/YYYY.MM → qa` and `→ develop` (and to any newer active release lines) |
| `sync-qa-to-develop.yaml` | Push to `qa` | Opens auto-PR `qa → develop`, auto-merge if clean |
| `drift-check.yaml` | Daily cron | Alerts if `develop` falls behind `qa` |

Workflows live in `.github/workflows/` once the app is scaffolded.

---

## 5. Guard rails to observe

| Guard rail | Observable signal |
|---|---|
| GR1 — qa → develop auto-sync | A PR labeled `auto-sync` appears in PR list moments after any push to qa, and auto-merges |
| GR2 — Forward-port batched on tag | A PR labeled `forward-port` appears moments after a tag push, merging release branch contents into qa + develop |
| GR3 — Drift detector | If qa moves and the sync fails, a GitHub Issue is opened by the daily cron |
| GR4 — Branch protection | Trying to push directly to `master` / `qa` / `develop` returns an error from the GitHub API |

---

## 6. Validation phases

Each phase has an explicit "**done when**" criterion. Don't move to the next until the current one is observed working.

### Phase 1 — Setup
Scaffold the app, kind cluster, runner, base workflows.
**Done when:** pushing to `develop` triggers `build-and-push.yaml`, image lands in ghcr.io, Helm deploys to `tbs-dev`, and `http://dev.tbs.local/users` returns `[]`.

### Phase 2 — First feature flow
Add a new endpoint via `feat/`. Promote develop → qa, test on `qa.tbs.local`, then qa → master, tag, promote to `prod.tbs.local`.
**Done when:** all three URLs (`dev`, `qa`, `prod`) serve the new endpoint and the immutable tag `v2026.05.0` exists in Git.

### Phase 3 — First release line + patch
After Phase 2, fork `release/2026.05` from the tag. Introduce a bug, fix via `patch/`, observe tag `v2026.05.1`, promote to prod, observe forward-port PR auto-open.
**Done when:** `prod.tbs.local` runs `v2026.05.1`, AND auto-PR appears merging release branch back to qa + develop.

### Phase 4 — Multi-version simulation
Cut a `v2026.08.0` major release. Fork `release/2026.08`. Keep `release/2026.05` alive in parallel. Patch both lines simultaneously.
**Done when:** two `release/*` branches exist with independent patch tags, and forward-port targets both qa and the newer release line.

### Phase 5 — Guard rail proof
Deliberately break: try to push directly to master; push to qa and verify auto-sync fires; simulate a divergence and watch the drift detector raise an issue.
**Done when:** each guard rail behavior is observed at least once.

### Phase 6 — Ongoing iteration
Add new requirements to the codebase over time (see Part 2 doc) to keep exercising the strategy.

---

## 7. What success looks like at the end

Six things must be demonstrably true in this repo:

1. The four long-lived branches all exist and never drift outside their roles.
2. Every customer-facing deploy is from an immutable tag, never a branch directly.
3. Patches for an old release line stay scoped to that line, never contaminating qa or master.
4. Forward-port automation flows patch-line fixes back without manual cherry-picking.
5. Branch protection prevents the historical anti-patterns (direct pushes, force-pushes to long-lived branches).
6. All of the above is achievable with self-hosted runner + free tooling — no AWS, no paid infra.

---

## 8. Out of scope for this sandbox

| Not covered here | Reason |
|---|---|
| Multi-customer real cluster separation | One kind cluster with namespaces is functionally equivalent for learning |
| Real ECR / OIDC | ghcr.io behaves identically for the workflow validation |
| Real K8s HA / rolling-update at scale | Single-node kind still demonstrates rolling behavior |
| Real DB migration tooling (Flyway/Liquibase) | The strategy is branch-focused; DB migration rules from the v2 doc apply unchanged |
| QE team simulation | Sole developer self-tests in the qa namespace |

---

## 9. References

- **Strategy v2 (the doc this validates):** See README refcard at project root
- **Codebase context (Part 2):** `./02-codebase-context.md`