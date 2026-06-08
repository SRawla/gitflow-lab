# Part 2 — Demo Codebase & Project Context

## What this app is

A minimal Spring Boot REST service that manages two entities — **User** and **Location** — with a one-to-many relationship (one location can have many users; each user belongs to one location). It is intentionally tiny: the point is to exercise the branching strategy, not to build a real product.

> **Reference for the strategy this codebase exists to validate:** [`01-branching-strategy-context.md`](./01-branching-strategy-context.md)

---

## 1. Tech stack

| Layer | Choice | Version |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x (latest stable in this minor) |
| Build tool | Maven | 3.9.x |
| Web | Spring Web (embedded Tomcat) | bundled |
| Persistence | Spring Data JPA + Hibernate | bundled |
| DB | PostgreSQL | 16 (containerized) |
| Migrations (Phase 1) | Flyway | V1–V6 scripts in `src/main/resources/db/migration/` |
| Container | Docker (single-stage, pre-built JAR + JRE Alpine) | — |
| Registry | GitHub Container Registry (`ghcr.io`) | free for personal repos |
| Local K8s | kind (Kubernetes in Docker) | latest stable |
| Deploy | Helm chart in repo (`chart/`) | helm 3 |
| Ingress | nginx-ingress (installed once in the kind cluster) | latest stable |
| CI/CD | GitHub Actions on self-hosted runner | runner installed on this laptop |

---

## 2. Domain model

Two entities. The simplest version that has a relation worth testing.

### `Location`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID (PK) | server-generated |
| `name` | varchar(120) | unique, not null |
| `city` | varchar(120) | nullable |
| `country` | varchar(80) | nullable |
| `createdAt` | timestamp | auto |

### `User`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID (PK) | server-generated |
| `name` | varchar(120) | not null |
| `email` | varchar(180) | unique, not null |
| `location_id` | UUID (FK → Location.id) | nullable; many users may share one location |
| `createdAt` | timestamp | auto |

### `Course`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID (PK) | server-generated |
| `name` | varchar(120) | not null |
| `description` | text | nullable |
| `createdAt` | timestamp | auto |

### `UserCourseAssignment` (on develop only)
| Field | Type | Notes |
|---|---|---|
| `userId` | UUID (composite PK) | FK to User |
| `courseId` | UUID (composite PK) | FK to Course |
| `assignedAt` | timestamp | auto |

### Relationship

```
Location 1 ─── ∞ User
Course ∞ ─── ∞ User   (via user_course_assignment join table)
```

JPA mapping: `@ManyToOne` from `User` to `Location` with `fetch = LAZY`. Bi-directional optional (do not add `@OneToMany` on Location unless a future endpoint actually needs it; keeps the entity slim).

---

## 3. Initial REST endpoints (Phase 1 scope)

Standard CRUD shape. Keep responses minimal — the point is the deploy flow, not the API surface.

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| GET | `/users` | — | `User[]` | List all |
| POST | `/users` | `{name, email, locationId?}` | `User` (201) | Create |
| GET | `/users/{id}` | — | `User` or 404 | Fetch one |
| GET | `/locations` | — | `Location[]` | List all |
| POST | `/locations` | `{name, city?, country?}` | `Location` (201) | Create |
| GET | `/locations/{id}` | — | `Location` or 404 | Fetch one |
| GET | `/courses` | — | `Course[]` | List all |
| POST | `/courses` | `{name, description?}` | `Course` (201) | Create |
| GET | `/courses/{id}` | — | `Course` or 404 | Fetch one |
| PUT | `/courses/{id}` | `{name, description?}` | `Course` | Update |
| DELETE | `/courses/{id}` | — | 204 | Delete |
| GET | `/courses/count` | — | `long` | Count all courses |
| GET | `/courses/{id}/enrollees` | — | `UUID[]` | List enrolled users (develop only) |
| POST | `/courses/{id}/enrollees` | `{userId}` | 201 | Enroll user (develop only) |
| DELETE | `/courses/{id}/enrollees/{userId}` | — | 204 | Unenroll user (develop only) |
| GET | `/actuator/health` | — | Spring Boot health | For K8s probes |

Future requirements (Phase 2 onward — added later as branching exercises):
- Update / delete endpoints
- Search by location
- Pagination
- A deliberate bug to fix via patch
- A new field migration to exercise schema change rules

---

## 4. Project structure (Maven layout)

```
test-branching-strategy/
├── .ai/                          # context for AI + dev
│   ├── CLAUDE.md
│   ├── 01-branching-strategy-context.md
│   ├── 02-codebase-context.md
│   └── 03-validation-log.md
├── .github/
│   └── workflows/
│       ├── pr.yaml                # PR checks (branch name validation + build)
│       ├── build.yaml             # Manual dispatch: build from any ref
│       ├── build-on-tag.yaml      # Auto: build on v* tag push
│       ├── deploy-dev.yaml        # Manual: deploy image to tbs-dev
│       ├── deploy-qa.yaml         # Manual: deploy image to tbs-qa
│       ├── deploy-prod.yaml       # Manual: deploy tag to tbs-prod
│       ├── release-pr.yaml        # Manual: open PR qa→master
│       ├── release-tag.yaml       # Manual: cut vYYYY.MM.0 + fork release branch
│       ├── patch-tag.yaml         # Manual: auto-increment patch tag
│       ├── auto-qa-pr.yaml        # Auto: cherry-pick develop→qa
│       ├── sync-qa-to-develop.yaml # GR1: cherry-pick qa→develop
│       └── forward-port.yaml      # GR2: tag push→PRs to qa+develop
├── chart/
│   └── tbs/                      # Helm chart
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── values-dev.yaml
│       ├── values-qa.yaml
│       ├── values-prod.yaml
│       └── templates/
├── src/
│   ├── main/java/com/sh/tbs/
│   │   ├── TbsApplication.java
│   │   ├── common/
│   │   │   └── ResourceNotFoundException.java
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java   # Bug Fix 2a
│   │   ├── location/
│   │   │   ├── Location.java
│   │   │   ├── LocationRepository.java
│   │   │   ├── LocationService.java
│   │   │   └── LocationController.java
│   │   ├── course/
│   │   │   ├── Course.java
│   │   │   ├── CourseRepository.java
│   │   │   ├── CourseService.java
│   │   │   ├── CourseController.java
│   │   │   └── dto/
│   │   │       ├── CourseRequest.java
│   │   │       └── CourseResponse.java
│   │   ├── enrollment/               # Feature 2 (develop only)
│   │   │   ├── EnrollRequest.java
│   │   │   ├── EnrollmentRepository.java
│   │   │   ├── EnrollmentService.java
│   │   │   ├── UserCourseAssignment.java
│   │   │   └── UserCourseId.java
│   │   └── user/
│   │       ├── User.java
│   │       ├── UserRepository.java
│   │       ├── UserService.java
│   │       └── UserController.java
│   └── main/resources/
│       ├── application.yaml
│       └── db/migration/
│           ├── V1__create_location.sql
│           ├── V2__create_user.sql
│           ├── V3__seed_data.sql
│           ├── V4__create_course.sql
│           ├── V5__seed_courses.sql
│           └── V6__create_user_course.sql  # Feature 2 (develop only)
├── Dockerfile                    # Single-stage: JRE Alpine + pre-built JAR
├── pom.xml
└── README.md
```

Package convention: `com.sh.tbs` (tbs = test-branching-strategy).

---

## 5. Local development requirements

Assumed installed on the dev laptop:

| Tool | Why | Install check |
|---|---|---|
| JDK 21 | Build + run | `java -version` |
| Maven 3.9+ | Build | `mvn -version` |
| Docker Desktop | Docker images, kind runs on it | `docker version` |
| `kind` | Local Kubernetes | `kind version` |
| `kubectl` | Talk to cluster | `kubectl version --client` |
| `helm` 3 | Deploy chart | `helm version` |
| GitHub Actions self-hosted runner | Wired to your repo | listed in GitHub repo Settings → Actions → Runners |

> If something is missing, install via Chocolatey on Windows (`choco install ...`). The setup is one-time.

---

## 6. Build & run cheat sheet

### Pure local (no Docker, no K8s, just laptop)

```bash
# 1. Start a local Postgres for development
docker compose -f deploy/docker-compose.local.yml up -d

# 2. Run the app
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. Hit it
curl http://localhost:8080/actuator/health
```

### Containerized (build image, run via docker)

```bash
mvn -DskipTests clean package
docker build -t tbs:local .
docker run --rm -p 8080:8080 --network host \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tbs \
  tbs:local
```

### Full strategy mode (kind + helm + ingress)

```bash
# 1. Create kind cluster (one-time)
kind create cluster --config deploy/kind-cluster.yaml --name tbs

# 2. Install nginx-ingress (one-time)
kubectl apply -f https://kind.sigs.k8s.io/examples/ingress/deploy-ingress-nginx.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=120s

# 3. Add to /etc/hosts (one-time, as admin):
# 127.0.0.1   dev.tbs.local qa.tbs.local prod.tbs.local

# 4. Deploy to dev namespace
kubectl create namespace tbs-dev --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install tbs-dev chart/tbs \
  -f chart/tbs/values-dev.yaml \
  -n tbs-dev

# 5. Verify
curl http://dev.tbs.local:8080/actuator/health
```

The GitHub Actions workflows automate steps 4–5 for every relevant push.

---

## 7. Helm chart key decisions

- **One chart, multiple values files.** `values-{dev,qa,prod}.yaml` differ on: image tag, replica count, resource limits, ingress host, DB connection.
- **Postgres bundled as a sub-chart for dev / qa.** Prod env uses an external Postgres connection (still local, but separately deployed) to mimic real customer envs.
- **Image tag pinning per env values file.** This is the file `promote.yaml` workflow edits to deploy a new tag.
- **Resource limits set low** (e.g. 200m CPU, 256Mi RAM) — kind cluster runs on a laptop.

---

## 8. Image tag scheme

| Ref type | Image tag |
|---|---|
| Version tag (`v*`) | Tag value directly (e.g. `v2026.06.0`) |
| Branch | `{sanitized-branch}.{run_number}` (e.g. `develop.15`, `qa.16`) |

---

## 9. Phase 1 deliverable definition

After Phase 1 setup is done, **these must be true**:

| Check | Verify by |
|---|---|
| App runs locally with `mvn spring-boot:run` | `curl http://localhost:8080/actuator/health` returns `UP` |
| Docker image builds | `docker build .` succeeds; final image &lt; 250 MB |
| kind cluster runs and nginx-ingress is up | `kubectl get pods -A` shows ingress-nginx ready |
| `dev.tbs.local`, `qa.tbs.local`, `prod.tbs.local` resolve to 127.0.0.1 | `ping dev.tbs.local` returns 127.0.0.1 |
| GitHub Actions self-hosted runner is online | GitHub repo → Settings → Actions → Runners shows ✓ |
| `build-and-push.yaml` triggers on push to `develop` and deploys to `tbs-dev` namespace | After a push: `kubectl get pods -n tbs-dev` shows the new image |
| `GET /users` returns `[]` via `http://dev.tbs.local/users` | curl returns 200 with `[]` |

Once Phase 1 is done, the rest of the validation phases (Part 1 doc §6) become exercise scripts the developer can run.

---

## 10. Future requirements (placeholders to evolve the demo)

Adding these later gives more scenarios to practice the strategy:

| Requirement | Strategy scenario it exercises |
|---|---|
| Add `PUT /users/{id}` | Standard feature flow |
| Add a deliberate bug, fix it | First QE-found-bug fix on qa |
| "Customer" reports a bug after prod deploy | First patch flow on `release/YYYY.MM` |
| Critical bug needing immediate fix | Hotfix flow |
| Schema change (add `phone` to User) | DB migration interaction (one of the open points) |
| Two release lines alive | Multi-version patch flow |
| Same bug exists on two release lines | Forward-port to multiple lines |
| Roll back a bad release | Image-only rollback via tag re-pin |

The codebase grows minimally — only as much as needed to create the next branching scenario worth practicing.
