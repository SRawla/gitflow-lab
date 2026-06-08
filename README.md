# test-branching-strategy

A personal sandbox to **practically validate** a Branching Strategy (Model C) end-to-end on a tiny Spring Boot + Postgres service running on local Kubernetes (`kind`), with real GitHub Actions wired to a self-hosted runner.

> The actual app is intentionally minimal — the value is in the workflow, not the code.

## Where the context lives

All project context is in [`.ai/`](./.ai/).

| File | Purpose |
|---|---|
| [`.ai/CLAUDE.md`](./.ai/CLAUDE.md) | Entry index — start here |
| [`.ai/01-branching-strategy-context.md`](./.ai/01-branching-strategy-context.md) | The strategy plan: branches, tags, envs, workflows, guard rails, validation phases |
| [`.ai/02-codebase-context.md`](./.ai/02-codebase-context.md) | The code plan: Spring Boot + JPA, domain model, project structure, Helm/K8s, build & run |

## Tech stack (one line)

Spring Boot 3.x · Java 21 · Maven · PostgreSQL 16 · Docker → ghcr.io · kind (local K8s) · Helm 3 · GitHub Actions on a self-hosted runner.

## Status

Active. App scaffolded (Spring Boot + JPA + Flyway), all workflow actions implemented, local kind cluster running with dev/qa/prod namespaces. Currently validating the full release cycle end-to-end.

---

## Branching Strategy & Workflow — Complete Reference

The whole exercise to understand the correct branching strategy and required workflow actions with guard rails for a microservice environment with multiple customers running on multiple versions / releases.

```
═══════════════════════════════════════════════════════════════════════════════════════════════
                    Branching Strategy v2 — Complete Flow Refcard
═══════════════════════════════════════════════════════════════════════════════════════════════


══ FEATURE DEVELOPMENT ════════════════════════════════════════════════════════════════════════

        feat/TICKET ─┐
        fix/TICKET  ─┼── PR ──► develop ──► auto cherry-pick PR ──► qa
        chore/TICKET─┘                      (auto-qa-pr.yaml)


══ QA CYCLE ═══════════════════════════════════════════════════════════════════════════════════

                              ┌─── QE finds bug ───┐
                              │                     │
                              ▼                     │
    qa ◄── cherry-picks ── develop          fix/TICKET (from qa)
                                                    │
                                                    ▼
                                              PR back to qa
                                                    │
                                                    ▼
                                            GR1: auto-sync
                                            qa → develop PR
                                        (sync-qa-to-develop.yaml)


══ MAJOR RELEASE (QA cycle ends) ══════════════════════════════════════════════════════════════

    ┌─────────────────────────────────────────────────────────────────────────────────┐
    │  Step 1: release-pr.yaml (manual dispatch)                                      │
    │           → opens PR: qa → master                                               │
    │                                                                                 │
    │  Step 2: review + approve + merge (manual)                                      │
    │           → master fast-forwards to qa HEAD                                     │
    │                                                                                 │
    │  Step 3: release-tag.yaml (manual dispatch, input: YYYY.MM)                     │
    │           → cuts tag vYYYY.MM.0 from master                                     │
    │           → forks branch release/YYYY.MM from that tag                          │
    │                                                                                 │
    │  Step 4: build-on-tag.yaml (AUTO on tag push)                                   │
    │           → builds image tagged vYYYY.MM.0, loads into kind                     │
    │                                                                                 │
    │  Step 5: deploy-prod.yaml (manual dispatch, input: tag)                         │
    │           → deploys vYYYY.MM.0 to tbs-prod                                      │
    │                                                                                 │
    │  Step 6: forward-port.yaml (AUTO on tag push)                                   │
    │           → opens PRs: release/YYYY.MM → qa + develop                           │
    └─────────────────────────────────────────────────────────────────────────────────┘

                qa ────────────► master ────────────► tag vYYYY.MM.0
                    (PR merge)          (tag+fork)        │
                                                          ├──► release/YYYY.MM (forked)
                                                          ├──► image built (auto)
                                                          └──► deploy to prod (manual)


══ PROD DEPLOY FAILURE ════════════════════════════════════════════════════════════════════════

    deploy-prod fails (image crash / boot failure)
                    │
                    ▼
        hotfix/TICKET (from release/YYYY.MM)
                    │
                    ▼
        PR → release/YYYY.MM → merge
                    │
                    ▼
        patch-tag.yaml (dispatch) → vYYYY.MM.1
                    │
                    ├──► build-on-tag (auto) → new image
                    ├──► deploy-prod (manual) → redeploy with vYYYY.MM.1
                    └──► forward-port (auto) → PRs to qa + develop


══ CUSTOMER BUG FIX ═══════════════════════════════════════════════════════════════════════════

    Customer reports issue on prod (running vYYYY.MM.N)
                    │
        ┌───────── severity? ─────────┐
        │                              │
        ▼                              ▼
    CRITICAL (prod down)         NORMAL (scheduled)
        │                              │
        ▼                              ▼
    hotfix/TICKET              patch/TICKET
    (fast-tracked,             (next patch cycle,
     from release/YYYY.MM)      from release/YYYY.MM)
        │                              │
        └──────── both merge into ─────┘
                        │
                        ▼
              release/YYYY.MM (PR + merge)
                        │
                        ▼
              patch-tag.yaml (dispatch)
                        │
                        ▼
              tag vYYYY.MM.N (auto-incremented)
                        │
         ┌──────────────┼──────────────┐
         ▼              ▼              ▼
    build-on-tag   forward-port    deploy-prod
    (auto)         (auto)          (manual dispatch)
                   → qa                │
                   → develop           ▼
                   → newer release/*   tbs-prod updated


══ MULTI-CUSTOMER (future state) ══════════════════════════════════════════════════════════════

    Shell-prod  ──► pinned to tag v2026.03.5  ──► release/2026.03 (alive)
    PWC-prod    ──► pinned to tag v2026.06.2  ──► release/2026.06 (alive)

    Each customer gets patches independently:
        patch → release/2026.03 → v2026.03.6 → deploy to Shell
        patch → release/2026.06 → v2026.06.3 → deploy to PWC

    Forward-port carries fixes from older → newer release lines too.


══ GUARD RAILS ════════════════════════════════════════════════════════════════════════════════

    GR1: sync-qa-to-develop.yaml    │ Every push to qa → auto PR to develop
    GR2: forward-port.yaml          │ Every tag push → auto PRs to qa + develop + newer release/*
    GR3: drift-check (future)       │ Daily: alert if develop falls behind qa > 24h
    GR4: branch protection          │ No direct push to master/qa/develop/release/*


══ BRANCH PROTECTION (sandbox) ════════════════════════════════════════════════════════════════

    ┌────────────────┬────────────┬─────────────┬────────────┐
    │ Branch         │ Approvals  │ CI required  │ Force push │
    ├────────────────┼────────────┼─────────────┼────────────┤
    │ develop        │ 0          │ Yes          │ No         │
    │ qa             │ 1 (self)   │ Yes          │ No         │
    │ master         │ 1 (self)   │ No           │ No         │
    │ release/*      │ 1 (self)   │ Yes          │ No         │
    └────────────────┴────────────┴─────────────┴────────────┘


══ WORKFLOW DISPATCH CHEATSHEET ════════════════════════════════════════════════════════════════

    ┌──────────────────────┬────────────────────────────────────────────┐
    │ Workflow              │ When to run                                │
    ├──────────────────────┼────────────────────────────────────────────┤
    │ release-pr.yaml      │ QA cycle ends, ready to promote to master  │
    │ release-tag.yaml     │ After qa→master PR merged (input: YYYY.MM) │
    │ patch-tag.yaml       │ After patch/hotfix merged to release/*     │
    │ build.yaml           │ Manual build from any ref (branch or tag)  │
    │ deploy-dev.yaml      │ Deploy specific image to dev               │
    │ deploy-qa.yaml       │ Deploy specific image to qa                │
    │ deploy-prod.yaml     │ Deploy specific tag to prod                │
    └──────────────────────┴────────────────────────────────────────────┘

    AUTO-TRIGGERED (no manual action):
    ┌──────────────────────┬────────────────────────────────────────────┐
    │ build-on-tag.yaml    │ Any v* tag push → builds image             │
    │ forward-port.yaml    │ Any v* tag push → PRs to qa + develop      │
    │ auto-qa-pr.yaml      │ Push to develop → cherry-pick PR to qa     │
    │ sync-qa-to-develop   │ Push to qa → cherry-pick PR to develop     │
    │ pr.yaml              │ Any PR → validates branch name + CI        │
    └──────────────────────┴────────────────────────────────────────────┘


══ ROLES — WHO TRIGGERS WHAT ══════════════════════════════════════════════════════════════════

    ┌──────────────────────┬─────────────┬─────────────────────────────────────────┐
    │ Workflow              │ Role        │ Context                                 │
    ├──────────────────────┼─────────────┼─────────────────────────────────────────┤
    │ build.yaml           │ Dev / Ops   │ Build image from any branch or tag      │
    │ deploy-dev.yaml      │ Dev         │ Deploy to dev for integration testing   │
    │ deploy-qa.yaml       │ Dev / Ops   │ Deploy to qa for QE testing             │
    │ release-pr.yaml      │ Tech Lead   │ Initiates release when QA cycle ends    │
    │ release-tag.yaml     │ Tech Lead   │ Cuts tag + forks release branch         │
    │ patch-tag.yaml       │ Tech Lead   │ Cuts patch tag after hotfix/patch merge │
    │ deploy-prod.yaml     │ Ops         │ Deploys specific tag to production      │
    └──────────────────────┴─────────────┴─────────────────────────────────────────┘

    AUTO (no human trigger):
    ┌──────────────────────┬──────────────────────────────────────────────────────┐
    │ build-on-tag.yaml    │ System builds image when tag is pushed               │
    │ forward-port.yaml    │ System opens PRs to sync fix to qa + develop         │
    │ auto-qa-pr.yaml      │ System cherry-picks develop commits to qa            │
    │ sync-qa-to-develop   │ System cherry-picks qa fixes back to develop         │
    │ pr.yaml              │ System validates branch naming + runs CI             │
    └──────────────────────┴──────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════════════════════════════
```
