# VyroFlix — Repository Agent Instructions

## Purpose

You are implementing **VyroFlix**, a low-cost, cloud-native video-on-demand platform. Read this file before changing the repository. It is the repository-wide implementation contract and must be used together with:

- `PROJECT_REQUIREMENTS.md` — product and acceptance requirements.
- `ARCHITECTURE.md` — service boundaries, data ownership, APIs, events, media flow, and deployment strategy.
- `apps/web/AGENTS.md` — Next.js frontend rules, if that file exists.
- `services/AGENTS.md` — Spring Boot backend rules, if that file exists.

If two instructions conflict, prefer the most specific file for the area being changed, then preserve the architecture and security rules in this file.

## Product Context

VyroFlix is a Netflix-style VOD platform. Its core media pipeline is:

```text
Admin/browser
  -> Video-Service
  -> Cloudflare R2/S3 raw upload
  -> video.uploaded.v1 Kafka event
  -> Encoding-Service + FFmpeg
  -> Cloudflare R2/S3 HLS outputs
  -> video.encoded.v1 Kafka event
  -> Streaming-Service
  -> Upstash Redis master-playlist-key cache
  -> short-lived signed HLS URL
  -> browser hls.js player via Cloudflare CDN
```

The backend control plane is Spring Boot. The video data plane is R2/S3 plus CDN. Video bytes must not pass through Spring Boot, Render, Vercel, or the API Gateway.

## Fixed Technology Decisions

### Frontend

- Next.js App Router.
- React and TypeScript with strict mode.
- Tailwind CSS.
- `hls.js` for browser HLS playback.
- Zod for runtime validation.
- React Hook Form for forms.
- TanStack Query where client-side server-state management is needed.
- Deploy to Vercel Hobby when online deployment is required.

### Backend

- Java 21.
- Spring Boot 3.x.
- Spring Cloud Gateway.
- Spring Security OAuth2 Resource Server/JWT validation.
- Spring Data JPA.
- Spring Kafka.
- Flyway.
- OpenAPI.
- Docker.
- Testcontainers.
- Micrometer/OpenTelemetry-compatible observability.

### Infrastructure

- Supabase PostgreSQL Free: metadata only; do not store video blobs in PostgreSQL.
- Supabase Auth: email/password and Google Sign-In.
- Upstash Redis Free: master-playlist keys, short-lived cache, entitlement cache, and rate limiting.
- Upstash Kafka Free: asynchronous event bus.
- Cloudflare R2: raw uploads and encoded HLS output.
- Cloudflare CDN: HLS delivery and edge caching.
- Render Free: Spring Boot hosting when online deployment is needed; local Docker Compose remains the zero-cost baseline.
- MinIO may replace R2 locally.

Treat free-tier quotas as configuration constraints, not guarantees. Never hardcode quota assumptions into business logic.

## Repository Structure

Prefer this structure:

```text
vyroflix/
  AGENTS.md
  PROJECT_REQUIREMENTS.md
  ARCHITECTURE.md
  apps/
    web/
  services/
    api-gateway/
    identity-service/
    content-service/
    video-service/
    encoding-service/
    streaming-service/
    history-service/
    search-service/
    recommendation-service/
  infra/
    docker/
    render/
    vercel/
    observability/
  docs/
  .env.example
  docker-compose.yml
```

For the first low-cost deployment, logical services may be combined into one Spring Boot deployment to reduce Render resource usage. Preserve package/domain boundaries so they can be separated later. Video-Service and Encoding-Service may remain local during early development because FFmpeg is CPU- and disk-intensive.

## Non-Negotiable Architecture Rules

- The API Gateway is the only public backend API entry point.
- The browser must never call internal microservice URLs directly.
- Each logical service owns its data and schema.
- No service may read another service's database directly.
- Do not introduce distributed database transactions.
- Use REST for immediate request/response operations.
- Use Kafka for cross-service state changes and asynchronous processing.
- Use a transactional outbox for business writes that publish events.
- Kafka consumers must be idempotent.
- Use versioned event names and schemas.
- Object storage/CDN is the media data plane.
- API Gateway and Spring services must never proxy HLS playlists or segments.
- Signed upload and playback URLs must be short-lived and must never be logged.
- Do not claim support for thousands of concurrent streams without load-test evidence.

## Service Responsibilities

### API Gateway

- Route `/api/v1/**` requests.
- Validate Supabase JWTs or enforce resource-server authentication.
- Apply CORS, rate limits, request-size limits, correlation IDs, and consistent errors.
- Do not contain domain logic or persistence.
- Do not proxy source uploads or HLS media.

### Identity Service

- Integrate with Supabase Auth.
- Support email/password and Google Sign-In through Supabase Auth.
- Maintain internal profile, role, and device-session data.
- Roles are `USER` and `ADMIN`.
- Never store or return plaintext passwords.

### Content-Service

- Own movies, series, seasons, episodes, genres, cast, artwork, and publication state.
- Publication states: `DRAFT`, `PUBLISHED`, `UNPUBLISHED`.
- Do not publish content until required media assets are `READY`.
- Produce catalog read models and publication events.

### Video-Service

- Own upload intents and raw-upload metadata.
- Generate pre-signed R2/S3 upload URLs.
- Support multipart upload coordination for large files where practical.
- Publish `video.uploaded.v1` only after the upload is verified.

### Encoding-Service

- Consume `video.uploaded.v1`.
- Download raw media from R2/S3.
- Run FFmpeg asynchronously.
- Produce multi-rendition HLS VOD output, such as 1080p, 720p, and 480p.
- Generate a master `.m3u8` and variant playlists with aligned keyframes.
- Upload playlists and segments to R2/S3.
- Publish `video.encoded.v1` only after outputs are verified.
- Mark jobs `QUEUED`, `PROCESSING`, `READY`, or `FAILED`.

### Streaming-Service

- Consume `video.encoded.v1`.
- Cache the master playlist object key in Upstash Redis.
- Validate authentication, publication status, and entitlement.
- Create playback sessions.
- Return a short-lived signed R2/S3 URL for the master playlist.
- Never serve the media itself.

### History Service

- Own progress, Continue Watching, watchlist, and completion records.
- Treat progress updates as idempotent upserts.
- Mark a video complete at the configured threshold, normally 90%.
- Emit viewing and watchlist events.

### Search Service

- Own its denormalized search index.
- Consume Content-Service publication/update events.
- Do not call Content-Service once per search result.
- Start with PostgreSQL full-text search; use OpenSearch only when needed.

### Recommendation Service

- Start with deterministic recommendations and trending rails.
- Consume playback/history/catalog events.
- Cache results.
- Fail open: recommendation failure must not break the home page.

## Exact Kafka Event Flow

### Upload-to-playback flow

1. Video-Service creates an upload intent.
2. Browser uploads the raw file directly to R2/S3.
3. Video-Service verifies completion and publishes `video.uploaded.v1`.
4. Encoding-Service consumes `video.uploaded.v1`.
5. Encoding-Service downloads the raw object, runs FFmpeg, uploads HLS output, and verifies the master playlist.
6. Encoding-Service publishes `video.encoded.v1`.
7. Streaming-Service consumes `video.encoded.v1` and stores the master playlist key in Redis.
8. Content-Service may publish the title only after the media reference is `READY`.
9. Content-Service publishes `catalog.title.published.v1`.
10. Search-Service and Recommendation-Service update their read models.
11. Viewer requests a playback session; Streaming-Service returns a short-lived signed manifest URL.

### Required event envelope

```json
{
  "eventId": "uuid",
  "eventType": "video.uploaded.v1",
  "occurredAt": "2026-09-13T04:50:00Z",
  "producer": "video-service",
  "correlationId": "uuid",
  "payload": {}
}
```

### Required event rules

- Use immutable, versioned event types.
- Include `eventId`, `eventType`, `occurredAt`, `producer`, `correlationId`, and `payload`.
- Consumers must deduplicate using `eventId` or an equivalent business idempotency key.
- Use retries and a dead-letter topic for poison messages.
- Do not put credentials, JWTs, signed URLs, passwords, raw payment details, or unnecessary PII in events.
- Use an outbox row written in the same transaction as the state change; a relay publishes it to Kafka.

## Security Rules

- Validate Supabase-issued JWTs at the gateway and enforce authorization downstream.
- Use issuer/audience/signature validation; do not trust an arbitrary forwarded user ID header.
- Enforce `USER` and `ADMIN` access at service boundaries.
- Store secrets only in environment variables or a secret manager.
- Provide `.env.example` with variable names only and safe placeholders.
- Never commit Supabase service-role keys, R2 secret keys, Kafka credentials, Redis tokens, JWT secrets, or OAuth client secrets.
- Use separate development and production credentials.
- Configure CORS to allow only the deployed frontend and local development origins.
- Restrict R2/S3 bucket access; use pre-signed URLs and least-privilege credentials.
- Keep raw uploads private and expose only encoded output through controlled signed access.
- Avoid logging authorization headers, cookies, tokens, signed URLs, passwords, or sensitive user data.

## Data and Persistence Rules

- Use Supabase PostgreSQL Free for metadata and service schemas.
- Keep video, HLS segments, images, and subtitles in R2/S3, not PostgreSQL.
- Use Flyway for every schema change.
- Use UUID primary keys and UTC timestamps.
- Add indexes for foreign keys, publication state, event IDs, timestamps, and user/asset lookups.
- Use optimistic locking where concurrent updates are possible.
- Enforce unique constraints for watchlist entries and idempotency records.
- Keep page sizes bounded.
- Avoid N+1 queries.
- If a separate database per service is too expensive, use separate PostgreSQL schemas locally/initially while preserving ownership boundaries.

## API Rules

- Public routes use `/api/v1`.
- Use resource-oriented nouns and appropriate HTTP status codes.
- Use request/response DTOs, never expose JPA entities.
- Validate DTOs using Jakarta Validation.
- Return RFC 7807 Problem Details or the repository's documented error envelope consistently.
- Document successful and error responses in OpenAPI.
- Enforce ownership for user-scoped resources.
- Use bounded offset or cursor pagination.
- Do not expose stack traces in production.

### Important endpoints

```text
POST /api/v1/videos/upload-intents
POST /api/v1/streaming/sessions
PUT  /api/v1/history/progress
GET  /api/v1/search
GET  /api/v1/catalog
```

### Playback response must include

- `sessionId`.
- Short-lived `manifestUrl`.
- `expiresAt`.
- `resumePositionSeconds`.

Do not return object-storage credentials.

## Frontend Rules

- Use Next.js App Router and React Server Components by default.
- Use Client Components only for browser APIs, hls.js, playback state, event handlers, and interactive forms.
- All API calls go through the API Gateway URL.
- Use Supabase Auth in the frontend for email/password and Google Sign-In.
- Pass/forward the resulting JWT securely to the backend according to the chosen cookie/token design.
- Do not store refresh tokens in localStorage.
- Dispose hls.js instances on unmount and prevent duplicate player initialization.
- Send playback progress every 15–30 seconds while actively playing, plus final best-effort updates on pause/end/navigation.
- Do not log signed media URLs.
- Implement loading, empty, error, retry, and unauthorized states.
- Keep the player responsible for media playback; do not download video through Next.js server functions.

## Deployment and Cost Rules

The default online deployment target is:

```text
Frontend: Vercel Hobby
Backend: Render Free or local Docker Compose
PostgreSQL + Auth: Supabase Free
Redis: Upstash Redis Free
Kafka: Upstash Kafka Free
Object storage + CDN: Cloudflare R2 + Cloudflare CDN
```

- Treat quotas as approximate and verify current provider limits before production use.
- Render Free services may sleep and have cold starts; design health checks and client retry states.
- Keep Encoding-Service local or separately deployed initially because FFmpeg is CPU-intensive.
- Do not use Vercel serverless functions for long-running encoding or media delivery.
- Do not use Render ephemeral disk as durable media storage.
- Do not store raw or encoded video in Supabase/PostgreSQL.
- Monitor Vercel bandwidth, Supabase database/egress, Upstash commands/messages, and R2 storage/operations.
- Include provider-neutral local alternatives: MinIO, local Kafka, PostgreSQL, and Redis through Docker Compose.
- A free deployment is a portfolio/demo environment, not a high-availability production environment.

## Local Development Requirements

Docker Compose should provide, where practical:

- Next.js web app.
- Spring Boot services or the initial combined backend.
- PostgreSQL.
- Redis.
- Kafka.
- MinIO as an R2/S3-compatible local object store.
- Optional Prometheus, Grafana, Loki, and tracing collector.

Local startup must not require paid cloud credentials. Cloud integrations must be switchable through profiles/configuration.

## Resilience Rules

- Configure connect, read, and response timeouts for outbound calls.
- Retry only transient and idempotent operations.
- Do not retry validation or authorization errors.
- Use circuit breakers for remote services where appropriate.
- Make encoding jobs retryable and idempotent.
- Use job-level locks or unique keys to prevent duplicate encoding for one asset.
- History progress may be retried asynchronously.
- Recommendation failures must not fail catalog responses.
- Handle expired signed URLs by requesting a new playback session.
- Add dead-letter handling for Kafka failures.

## Observability Requirements

Every backend service must provide:

- `/actuator/health`.
- Readiness and liveness indicators.
- Prometheus-compatible metrics.
- Structured JSON logs.
- Correlation ID propagation.
- Trace context propagation across HTTP and Kafka where supported.

Record metrics for:

- Request count, error count, and latency.
- Database pool usage.
- Redis cache hit/miss.
- Kafka publish/consume failures and lag.
- Upload and encoding job counts, duration, and failures.
- Playback authorization success/denial.
- Progress update rate.

## Testing Requirements

- Unit-test domain and application logic.
- Test controllers with MockMvc or WebTestClient.
- Use Testcontainers for PostgreSQL, Redis, Kafka, and MinIO-compatible integration tests where practical.
- Test Supabase JWT validation with representative signed test tokens; never use production credentials in tests.
- Test authorization boundaries: unauthenticated, USER, ADMIN.
- Test outbox publication and idempotent event consumption.
- Test duplicate `video.uploaded.v1` and `video.encoded.v1` deliveries.
- Test encoding failure, retry, and partial-output cleanup.
- Test signed URL expiry and playback-session refresh.
- Add Playwright tests for login, Google OAuth callback handling, browse, playback, watchlist, and Continue Watching.
- Add k6/Gatling tests for browse, search, playback authorization, and progress updates.

## Implementation Phases

### Phase 0 — Baseline

- Create monorepo and documentation.
- Add Docker Compose.
- Add environment configuration and CI.
- Add health endpoints, logging, and basic OpenAPI.
- Do not implement business features until the baseline starts reliably.

### Phase 1 — Viewer Vertical Slice

- Supabase Auth integration with email/password and Google Sign-In.
- API Gateway and initial backend deployment/local profile.
- Content-Service catalog reads and seeded data.
- Streaming-Service playback authorization using seeded HLS assets.
- Next.js browse, title details, and hls.js player.
- Basic History-Service progress and watchlist.

### Phase 2 — Upload and Encoding Pipeline

- Video-Service upload intents and pre-signed R2/S3 uploads.
- `video.uploaded.v1` event.
- Encoding-Service FFmpeg worker.
- Multi-bitrate HLS packaging and R2/S3 output.
- `video.encoded.v1` event.
- Streaming-Service Redis playlist-key caching.
- Admin processing-status UI.

### Phase 3 — Search, Recommendations, and Hardening

- Search read model and PostgreSQL full-text search.
- Deterministic recommendations and trending rails.
- Outbox relay, retries, dead-letter topics, and idempotency.
- Resilience4j policies, observability dashboards, and load tests.
- Optional OpenSearch migration.

### Phase 4 — Deployment and Scaling Demonstration

- Vercel frontend deployment.
- Render backend deployment or documented local-only mode.
- Supabase, Upstash, and Cloudflare integrations.
- Provider usage monitoring and cost guardrails.
- Optional separation of logical services into independent deployments.
- Kubernetes is optional and must not be introduced before the low-cost deployment works.

## Definition of Done

A change is complete only when:

- It follows the applicable requirements and architecture documents.
- It compiles/builds successfully.
- Formatting, linting, and static analysis pass.
- Unit and relevant integration tests pass.
- Database migrations are included for persistence changes.
- API and event documentation is updated.
- Docker/local configuration is updated for dependency changes.
- Security checks confirm no secrets or sensitive URLs are committed/logged.
- Failure, loading, retry, and unauthorized states are implemented where applicable.
- The change does not route video bytes through application servers.
- The implementation remains compatible with the documented free/minimum-cost deployment strategy.

## Agent Workflow

Before editing:

1. Read `AGENTS.md`, `PROJECT_REQUIREMENTS.md`, and `ARCHITECTURE.md`.
2. Inspect the existing repository and identify affected services.
3. State assumptions and identify missing contracts.
4. Propose a small implementation plan.
5. Confirm whether the change affects data ownership, API contracts, events, security, deployment, or cost.

While editing:

1. Make the smallest coherent change.
2. Preserve existing contracts unless a versioned change is required.
3. Add tests with the implementation.
4. Keep secrets and provider credentials out of source control.
5. Update documentation and `.env.example` when configuration changes.

After editing:

1. Run build, lint, static analysis, and relevant tests.
2. Check database migrations and event versions.
3. Check logs for secrets or signed URLs.
4. Verify local Docker Compose behaviour when infrastructure changes.
5. Report changed files, commands run, test results, known limitations, and next steps.
