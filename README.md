# Backend de la plateforme d'annonces

Spring Boot 4.1, Java 21, PostgreSQL 18. Le contrat est `../docs/api/openapi.yaml`, les choix sont dans `../docs/adr/0006-backend.md`.

## Démarrer en local

```bash
cp .env.example .env            # puis changer DB_PASSWORD
docker compose up -d            # ou podman compose up -d
set -a; . ./.env; set +a
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Santé : http://localhost:8080/actuator/health/liveness

Le profil `dev` charge les 48 annonces fictives et trois comptes d'essai (`candidat@example.org`, `employeur@example.org`, `moderateur@example.org`, mot de passe `motdepasse`). Il n'existe que pour le développement.

## Front branché sur ce backend

```bash
cd ../Kernel-vitrine && npm run start:api
```

## Tests

Les tests d'intégration démarrent un vrai PostgreSQL avec Testcontainers.

```bash
mvn test
# Avec Podman :
DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true mvn test
```

Couverture : `target/site/jacoco/index.html`.

Avec Podman, Ryuk étant désactivé, le conteneur de test reste après la suite : `podman ps -aq --filter label=org.testcontainers=true | xargs -r podman rm -f`.
