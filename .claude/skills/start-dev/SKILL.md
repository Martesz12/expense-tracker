---
name: start-dev
description: Start the local dev environment Docker containers. Asks whether to start only postgres (for local backend dev with mvn spring-boot:run) or the full stack (postgres + backend + frontend). Checks what is already running before starting anything.
---

## Steps

1. Run `docker compose ps --format json 2>/dev/null || docker compose ps` from the project root to see which containers are already running.

2. If all requested services are already up and healthy, report their ports and stop — no need to restart.

3. Ask the user which mode they want (if not already specified in their message):
   - **DB only** — starts just `postgres` (used when running the backend with `mvn spring-boot:run` locally)
   - **Full stack** — starts all services (`postgres`, `backend`, `frontend`)

4. Run the appropriate command from the project root:
   - DB only: `docker compose up -d postgres`
   - Full stack: `docker compose up -d`

5. For DB-only: wait for the postgres healthcheck by running `docker compose ps postgres` until status shows `healthy` (retry a few times with a short wait if needed).

6. Report what is running and their ports:
   - postgres → `localhost:5432` (db: `expensetracker`, user: `app`, password: `secret`)
   - backend → `localhost:8080`
   - frontend → `localhost:4200`
