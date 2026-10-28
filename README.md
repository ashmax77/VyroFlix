# VyroFlix

[![CI](https://github.com/ashmax77/VyroFlix/actions/workflows/ci.yml/badge.svg)](https://github.com/ashmax77/VyroFlix/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)
![Next.js 14](https://img.shields.io/badge/Next.js-14-black.svg)

VyroFlix is a portfolio video-on-demand platform. It shows how a streaming service can be built from domain-oriented Spring Boot services, event-driven workflows (Kafka with a transactional outbox), object storage with a CDN as the media data plane, and a Next.js viewer and admin app.

It is a **demo environment, not a production service**. It runs on free-tier providers, has no load-test evidence for high concurrency, and uses seeded or open-licensed media only.

## What works

- **Accounts:** email/password and Google sign-in through Supabase Auth. Roles (`USER`, `ADMIN`) and subscription plans (`FREE`, `BASIC`, `PREMIUM`) come only from server-controlled JWT claims.
- **Catalog:** movies and series with seasons and episodes, genres, cast and crew, collections, and publication states. Publishing requires encoded media.
- **Upload and encoding:** admins upload source video directly to object storage with pre-signed URLs. An FFmpeg worker produces multi-bitrate HLS and reports `READY` or `FAILED`.
- **Playback:** the viewer's session is authorised from local read models (published state, encoded media, plan limits). The player receives a short-lived manifest URL, resumes from saved progress, and ends the session when it closes.
- **Delivery:** a Cloudflare Worker validates HMAC tokens, rewrites child playlists, and serves segments from a private R2 bucket. Video bytes never pass through Spring or Vercel.
- **Personal:** watchlist, Continue Watching, and progress that persists across sessions.
- **Discovery:** search with genre, rating, and year filters; trending and personalised recommendation rails that fail open; curated collection rails.
- **Admin:** create, publish, unpublish, and delete titles (delete only when unpublished); upload and retry encoding; platform statistics from each owning service.
- **Operations:** Prometheus metrics, JSON logs on the hosted profile, correlation IDs, health and readiness probes, and an OpenAPI description.

## Known limitations

- **Live database outbox not verified.** The outbox writes to a per-schema table. Confirm on your Supabase project that rows are published (see the deployment guide).
- **Playlist tokens are bearer capabilities.** Anyone holding a live token can fetch that title's segments until the child token expires (4 hours by default).
- **Plan limits are concurrency only.** Subscription tiers do not yet gate individual titles. Plans are set manually in Supabase `app_metadata`; there is no payment integration.
- **Concurrency checks are not locked.** Two simultaneous playback requests for one user can both pass the stream limit. Acceptable for a demo; a database lock would fix it.
- **Load tests have not been run against the deployed stack.** The k6 scripts exist; no capacity figures are claimed.
- **Testcontainers tests need Docker.** They run in CI; they were not executed on the machine where this README was written.
- **Admin credit editing is API-only.** The admin UI manages titles and publication but not cast lists.
- **Cold starts.** The free backend sleeps. A keep-alive workflow reduces this, but GitHub's scheduler can delay pings.

## Architecture

```text
Browser (Next.js on Vercel)
   |  /api/v1/**  (rewritten to the backend)
   v
Spring Boot platform on Render (combined services, one JVM)
   identity · content · video · streaming · history · search · recommendation
   |                         |
   | Supabase Postgres       | Upstash Redis (cache) · Upstash Kafka (events)
   |                         |
   |  video.uploaded.v1  -->  encoding worker (FFmpeg, runs outside Render)
   |  video.encoded.v1   -->  streaming + content read models
   v
Cloudflare R2 (private)  <--  Cloudflare Worker (token check, playlist rewrite)  <--  player (hls.js)
```

Each logical service owns its own schema. Services never read each other's tables. Cross-service changes travel as versioned events (`docs/events/EVENT_CATALOG.md`). Synchronous calls are limited to playback authorisation (reading saved progress from History over loopback, which fails open).

### Playback flow

1. The player calls `POST /api/v1/streaming/sessions` with the viewer's token.
2. Streaming checks availability (its own read model), that the encoded master playlist exists, and the plan's concurrent-stream limit. It records the session.
3. The response carries `sessionId`, a manifest URL with a 15-minute HMAC token, `expiresAt`, and `resumePositionSeconds`.
4. The Worker validates the token, returns the master playlist with child tokens good for four hours, and serves segments.
5. On exit the player calls `POST /api/v1/streaming/sessions/{id}/end`, which frees the stream slot.

## Services

| Module | Responsibility |
|---|---|
| `services/api-gateway` | Local/Docker gateway: routing, CORS, rate limits, correlation IDs |
| `services/platform` | The combined backend deployed to Render; hosts every domain module |
| `services/identity-service` | User profiles and roles, Supabase sync |
| `services/content-service` | Titles, seasons, episodes, genres, people and credits, collections, publication |
| `services/video-service` | Upload intents, pre-signed uploads, upload verification |
| `services/encoding-service` | FFmpeg HLS worker, job status, retries |
| `services/streaming-service` | Playback authorisation, plan limits, session lifecycle, signed manifest URLs |
| `services/history-service` | Progress, Continue Watching, watchlist |
| `services/search-service` | PostgreSQL full-text search read model with filters |
| `services/recommendation-service` | Trending and personal rails, cached; idempotent counters |
| `services/common` | Security, outbox, messaging, storage, error envelope |
| `infra/cloudflare/hls-worker` | Token-validating HLS delivery Worker (Node tests included) |
| `apps/web` | Next.js viewer and admin application |

## Local development

Requirements: Java 21, Maven 3.9+, Node 20+, Docker (for Compose and Testcontainers).

```bash
# Backend tests (unit and controller tests; Testcontainers suites need Docker)
mvn -f services/pom.xml verify

# Frontend
cd apps/web
npm ci
npm run dev            # http://localhost:3000

# CDN Worker tests (no Docker needed)
node --test infra/cloudflare/hls-worker/test/worker.test.mjs
```

Copy `.env.example` to `.env` and fill in the variables for the services you run. Cloud credentials are never committed.

## Testing

| Layer | What it covers | Where it runs |
|---|---|---|
| Unit | Domain and application rules (playback limits, signing, idempotency, publication rules) | `mvn verify` |
| Controller | Authentication, ownership, and validation at the HTTP boundary (MockMvc) | `mvn verify` |
| Integration | Migrations and read models on PostgreSQL; duplicate event delivery | CI (Docker) |
| Worker | Token verification, expiry, tampering, playlist rewriting | `node --test` |
| End-to-end | Route protection and the sign-in redirect | `npx playwright test` |
| Load | Browse, search, playback authorisation, progress (k6) | `tests/load` (manual) |

## Documentation

- [Product requirements](docs/PROJECT_REQUIREMENTS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Deployment guide](docs/DEPLOYMENT_GUIDE.md)
- [API reference](docs/api/API_REFERENCE.md)
- [Event catalog](docs/events/EVENT_CATALOG.md)
- [Repository rules](AGENTS.md)

## License

Educational project. Use only media you have the rights to distribute.
