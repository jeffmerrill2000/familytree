# Family tree

Local genealogy app: React (Vite) talks only to the Spring Boot API. Neo4j is the database and runs in Docker.

## Prerequisites

- Java 21 (`JAVA_HOME` must point at a JDK 21). This Mac also has Java 25 on `PATH`; Maven uses `JAVA_HOME`.
- Maven 3.10+
- Node.js 26 and npm
- Docker Desktop
- `jq` (used by `scripts/seed.sh`)

## Database

```bash
cp .env.example .env
# set NEO4J_AUTH=neo4j/<password> and NEO4J_PASSWORD=<password>
docker compose up -d neo4j
docker compose ps
```

`.env` is not committed. Neo4j rejects a container variable named `NEO4J_PASSWORD` (it is not a server setting), so Compose maps that value to `FT_NEO4J_PASSWORD` for the healthcheck only.

Data is stored in the `neo4j_data` volume and survives `docker compose stop` / `docker compose up -d`.

## Backend

```bash
cd backend
mvn spring-boot:run
```

API: `http://localhost:8080`. Health: `http://localhost:8080/actuator/health`.

```bash
cd backend && mvn test
```

Integration tests start their own Neo4j 5.26 container with Testcontainers.

## Seed

With the API running:

```bash
./scripts/seed.sh
```

Prints the id of the youngest child (Iris Hale, born 2008), a half-sibling of the three Hale children.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The API base URL is `VITE_API_URL` in `frontend/.env`.

## Not in this increment

Family-tree accounts and authorization, provenance, ancestor/descendant endpoints, OpenAPI, a concurrency lock around the two-parent rule, and any AWS resources. The parent-link Cypher is safe inside one transaction and is not yet safe against two concurrent writers.
