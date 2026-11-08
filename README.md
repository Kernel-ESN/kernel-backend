# Kernel Consulting — listings platform API

REST API behind the Kernel Consulting listings platform: job offers and calls for tenders in Niger, candidate accounts and applications, employer spaces, email alerts and moderation.

The frontend lives in [kernel-vitrine](https://github.com/Kernel-ESN/kernel-vitrine).

**Stack:** Spring Boot 4, Java 21, PostgreSQL 18, Flyway migrations, Testcontainers for integration tests.

## Quick start

Requirements: **Java 21**, **Maven 3.9+**, and **Docker** or **Podman** to run PostgreSQL.

```bash
git clone https://github.com/Kernel-ESN/kernel-backend.git
cd kernel-backend

cp .env.example .env                 # then set DB_PASSWORD
docker compose up -d                 # PostgreSQL (or: podman compose up -d)
set -a; . ./.env; set +a             # load the variables into the shell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API listens on http://localhost:8080. To check that it is up, open http://localhost:8080/actuator/health/liveness.

The `dev` profile loads 48 sample listings and three test accounts, all with the password `motdepasse`:

| Account | Role |
|---|---|
| `candidat@example.org` | Candidate |
| `employeur@example.org` | Employer, with a verified sample organisation |
| `moderateur@example.org` | Moderator |

This profile is for local development only.

## Connecting the frontend

In a clone of [kernel-vitrine](https://github.com/Kernel-ESN/kernel-vitrine):

```bash
npm ci
npm run start:api
```

The Angular dev server sends `/api` calls to this API on port 8080.

## Configuration

| Variable | Purpose |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL database and credentials |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | First moderator account, created at startup if missing (password: 12 characters minimum) |

`.env` is ignored by git; never commit it.

## Tests

Integration tests start a real PostgreSQL with Testcontainers.

```bash
mvn test
```

With Podman instead of Docker:

```bash
DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true mvn test
```

With Ryuk disabled, the test container is left behind after the run. Remove it with:

```bash
podman ps -aq --filter label=org.testcontainers=true | xargs -r podman rm -f
```

The coverage report is written to `target/site/jacoco/index.html`.
