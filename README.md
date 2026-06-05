# test-branching-strategy

A personal sandbox to **practically validate** the SkyHive Branching Strategy v2 (Model C) end-to-end on a tiny Spring Boot + Postgres service running on local Kubernetes (`kind`), with real GitHub Actions wired to a self-hosted runner.

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

Pre-scaffold. Context docs only. Next step: Phase 1 setup (app + Dockerfile + Helm chart + kind cluster + base workflows).
