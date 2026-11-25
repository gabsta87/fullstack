# Venus

Directory of advertisers ("workers") browsed by visitors and clients. Advertisers rent the publication of their
profile; the site never handles appointments or exchanges between visitors and advertisers. The site is free for now
(no payment code is active).

| Part | Stack | Docs |
|---|---|---|
| `front/` | Angular 22, Ionic 8, PrimeNG 22, Node 24, served by nginx | [front/README.md](front/README.md) |
| `serv/` | Spring Boot 4.1, Java 21, Gradle 8.14 | [serv/README.md](serv/README.md) |
| Database | MySQL 9.7 LTS (`ddl-auto: update`) | |
| `ops/nas/` | Read-only log access to the staging NAS | [ops/nas/README.md](ops/nas/README.md) |

Tasks are tracked in [ROADMAP.md](ROADMAP.md); working rules (branches, commits, tests) are in [CLAUDE.md](CLAUDE.md).

## Architecture

```
browser ── nginx (front container, :8888 → :8081)
             ├── /         Angular app (static files)
             ├── /api/**   → backend :8080 (REST + SSE)
             └── /media/** → public media volume (served with a sandbox CSP)
backend ── MySQL (venus-db), media volumes (public / private certification photos)
phpMyAdmin :8082 (local network only)
```

- Authentication: stateless JWT (HS256), checked against the database on every request (banned account or revoked
  token → refused). Public routes ignore an invalid token instead of failing.
- Live updates: Server-Sent Events, `/api/account/stream` (own account) and `/api/admin/stream` (admin dashboard).
- Languages: 9 site languages (fr, en, de, es, pt, it, ru, ua, ar). UI texts in `front/src/assets/lang/data.json`,
  email texts in `serv/src/main/resources/messages*.properties`, legal texts in the database.

## Run locally

Requires Docker Desktop. Create `.env.local` once from `.env.example` (local values, never committed):

```bash
docker compose -p venus-local --env-file .env.local up -d --build   # site on http://localhost:8888
docker logs -f venus-backend
docker compose -p venus-local --env-file .env.local down            # data is kept in the volumes
```

For development with live reload, run the backend (`cd serv && ./gradlew bootRun`) and the front
(`cd front && npm start`, proxied to the backend by `proxy.conf.json`).

## Configuration

Variables read by `docker-compose.yml`:

| Variable | Purpose |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` | MySQL database and accounts |
| `JWT_SECRET` | JWT signing key, at least 32 characters (`openssl rand -base64 48`); startup fails otherwise |
| `JWT_EXPIRATION` | Token lifetime, ISO-8601 duration (default `P30D`) |
| `EMAIL_VERIFICATION_REQUIRED` | `true` to require email verification before using an account (default `false`) |
| `PHPMYADMIN_BIND` | Interface phpMyAdmin listens on (default `0.0.0.0`; `127.0.0.1` on a public server) |

## Deployment

Every push to `master` runs `.github/workflows/deploy.yml`: backend and front tests on GitHub runners, then deployment
on the staging NAS (self-hosted runner) — https://venustest.synology.me. Production (Infomaniak) does not exist yet.
Secrets (`JWT_SECRET`, database passwords) are GitHub secrets.
