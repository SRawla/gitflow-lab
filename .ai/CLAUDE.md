# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A personal sandbox to practically validate the **SkyHive Branching Strategy v2 (Model C)** end-to-end on a minimal Spring Boot + PostgreSQL service running on a local `kind` cluster, with real GitHub Actions on a self-hosted runner. The app itself is intentionally trivial — the value is in the workflow.

- **Owner / sole reviewer:** suleman.ravla@gmail.com (self-approves PRs — deliberate, documented deviation from the two-approver production strategy)
- **Strategy reference doc:** `C:\SkyHive\ENT-2-0\CodeBase\.helper-resource\branching-strategy-v2\branching-strategy-guide.html`
- **Deeper context:** [`01-branching-strategy-context.md`](./01-branching-strategy-context.md) (branches, workflows, guard rails, validation phases) and [`02-codebase-context.md`](./02-codebase-context.md) (domain model, project structure, Helm, image tagging)

---

## Build & Run Commands

### Prerequisites
```
java -version        # must be JDK 21
mvn -version         # must be 3.9+
docker version
kind version
kubectl version --client
helm version
```

### Local development (no Docker, no K8s)
```bash
# Start local Postgres
docker compose -f deploy/docker-compose.local.yml up -d

# Run app
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Verify
curl http://localhost:8080/actuator/health
```

### Build & test
```bash
mvn verify                        # compile + test
mvn -DskipTests clean package     # build jar only
```

### Run a single test
```bash
mvn test -Dtest=UserControllerTest
mvn test -Dtest=UserControllerTest#shouldReturnEmptyList
```

### Build Docker image
```bash
mvn -DskipTests clean package
docker build -t tbs:local .
docker run --rm -p 8080:8080 --network host \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tbs \
  tbs:local
```

### kind cluster + Helm deploy (full strategy mode)
```bash
# One-time cluster setup
kind create cluster --config deploy/kind-cluster.yaml --name tbs
kubectl apply -f https://kind.sigs.k8s.io/examples/ingress/deploy-ingress-nginx.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=120s
# Add to hosts file (as admin): 127.0.0.1   dev.tbs.local qa.tbs.local prod.tbs.local

# Deploy to dev namespace
kubectl create namespace tbs-dev --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install tbs-dev chart/tbs -f chart/tbs/values-dev.yaml -n tbs-dev

# Verify
curl http://dev.tbs.local:8080/actuator/health
curl http://dev.tbs.local:8080/users
```

---

## Architecture

### App (pre-scaffold — this is the target structure)

**Package root:** `com.sh.tbs`  
**Two domain packages:** `user/` and `location/`, each with `Entity`, `Repository`, `Service`, `Controller`.  
**Relationship:** `User @ManyToOne(fetch=LAZY) → Location` (one location, many users).  
**DB migrations:** `ddl-auto=update` for Phase 1; Flyway later.  
**Profiles:** `local` profile uses `application-local.yaml` (points at Docker Compose Postgres).

### REST API (Phase 1 scope)
- `GET/POST /users`, `GET /users/{id}`
- `GET/POST /locations`, `GET /locations/{id}`
- `GET /actuator/health` (K8s probes)

### Helm chart (`chart/tbs/`)
One chart, four values files (`values.yaml` + `values-{dev,qa,prod}.yaml`). The `promote.yaml` workflow edits the image tag key in the target env's values file. Dev/QA use a bundled Postgres sub-chart; prod uses an external connection.

### GitHub Actions workflows (`.github/workflows/` — added in Phase 1)
| File | Trigger | Purpose |
|---|---|---|
| `pr.yaml` | PR open | Branch-name regex + `mvn verify` |
| `build-and-push.yaml` | Push to `develop` / `qa` / `release/**` | Build image, push to ghcr.io, Helm deploy to matching namespace |
| `promote.yaml` | Manual dispatch | Pin a tag to an env values file, open PR |
| `release-tag.yaml` | Manual dispatch | Cut `vYYYY.MM.N` from `release/YYYY.MM` |
| `forward-port.yaml` | Tag push `v*` | Auto-PR: release branch → qa → develop |
| `sync-qa-to-develop.yaml` | Push to `qa` | Auto-PR qa → develop, auto-merge if clean |
| `drift-check.yaml` | Daily cron | Open issue if develop falls behind qa |

### Branch model
| Branch | Role |
|---|---|
| `master` | Latest stable; fast-forwarded from qa at each major release |
| `qa` | Release candidate |
| `develop` | Feature integration |
| `release/YYYY.MM` | Long-lived patch home per release line |
| `feat/*`, `fix/*`, `chore/*` | Temporary, off `develop` |
| `patch/*`, `hotfix/*` | Temporary, off `release/YYYY.MM` |

### Image tag scheme
| Branch | Tag format |
|---|---|
| `develop` | `develop.{run}.{sha}` |
| `qa` | `qa.{run}.{sha}` |
| `release/YYYY.MM` | `patch.{run}.{sha}` |
| Git tag `vYYYY.MM.N` | Immutable; used for all prod deploys |

### Environments (3 namespaces in one kind cluster)
| Env | Namespace | Hostname | Pinned to |
|---|---|---|---|
| dev | `tbs-dev` | `dev.tbs.local` | `develop` (auto-deploy on push) |
| qa | `tbs-qa` | `qa.tbs.local` | `qa` (auto-deploy on push) |
| prod | `tbs-prod` | `prod.tbs.local` | A chosen immutable tag |

---

## Current status

**Pre-scaffold.** Context docs only. Next step: Phase 1 — scaffold app + Dockerfile + Helm chart + kind cluster + base workflows. Done when `http://dev.tbs.local/users` returns `[]` after a push to `develop` triggers the build-and-push workflow.
