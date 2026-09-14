# VyroFlix — Project Requirements (Final)

## 1. Overview

VyroFlix is a cloud-native, video-on-demand streaming platform built with Spring Boot microservices and a Next.js/React web frontend.

The platform must allow customers to:
- Create an account and authenticate securely.
- Browse titles, genres, collections, and trending content.
- Search movies and series.
- View title details, seasons, episodes, cast, and related content.
- Add/remove content from a personal watchlist.
- Play HLS video using a browser player.
- Resume playback from saved progress.
- Receive basic personalised recommendations.
- Manage subscription-plan state in MVP form.

The platform must allow administrators to:
- Create, edit, publish, and unpublish titles.
- Upload source-video assets.
- Trigger and monitor transcoding jobs.
- Manage title metadata, poster images, genres, seasons, and episodes.
- View basic platform analytics.

## 2. Product Goals

- Demonstrate a production-minded Spring Boot microservice architecture.
- Demonstrate asynchronous event-driven workflows with Kafka and the transactional outbox pattern.
- Deliver video through AWS S3 (or MinIO locally) and CDN rather than Spring services.
- Provide a polished responsive consumer web application.
- Support local development using Docker Compose.
- Be incrementally deployable to Kubernetes or AWS.
- Include testability, security, documentation, and observability.

## 3. Non-Goals

The initial version must not:
- Host copyrighted or unlicensed content.
- Implement DRM.
- Build a real payment gateway before the core platform works.
- Build a real ML recommendation engine in Phase 1.
- Require Kubernetes before Docker Compose works.
- Claim Netflix-level throughput without load-test evidence.

Use open-licensed sample videos, trailers, or generated test media only.

## 4. Personas

### Viewer
A registered person who browses, searches, streams content, creates a watchlist, and resumes viewing.

### Administrator
An authorised user who creates and publishes content, manages metadata, uploads source video, and monitors processing status.

### Platform Operator
A developer/operator who monitors service health, logs, traces, processing status, and event-consumer health.

## 5. Functional Requirements

### FR-01: Authentication
- Users can register using email and password.
- Users can log in and log out.
- JWT access tokens must expire quickly.
- Refresh tokens must support session renewal.
- Passwords must be hashed using BCrypt or Argon2.
- Roles: USER and ADMIN.
- Unauthenticated users cannot access protected APIs.
- Admin-only APIs must reject USER role access.

### FR-02: Catalog (Content-Service)
- Catalog supports MOVIE and SERIES title types.
- A movie has one playable media asset.
- A series has seasons and episodes.
- Titles have title, synopsis, release year, maturity rating, duration, genres, cast, poster, backdrop, status, and tags.
- Only PUBLISHED titles are visible to viewers.
- Admins can create drafts and publish/unpublish titles.
- Home page supports curated rails such as Trending, Recently Added, Action, Drama, and Continue Watching.
- Content-Service must not publish a title unless required media assets are in READY state.

### FR-03: Search
- Users can search title name, genre, cast, tags, and synopsis.
- Search supports pagination.
- Search supports filters: content type, genre, release year, and maturity rating.
- Phase 1 may use PostgreSQL full-text search.
- Phase 2 should migrate search to OpenSearch.

### FR-04: Playback (Streaming-Service)
- A viewer can request a playback session for a published title or episode.
- Streaming-Service validates user authentication and entitlement.
- Streaming-Service creates an auditable playback session.
- Streaming-Service returns a short-lived pre-signed S3 URL to the HLS master playlist.
- The frontend loads the HLS manifest directly from S3/CDN.
- Spring services must not proxy media segments.
- Player supports play/pause, seek, volume, fullscreen, quality selection when available, and subtitle track selection when available.

### FR-05: Watch Progress (History Service)
- Client sends progress heartbeat every 15 to 30 seconds while actively playing.
- Progress must contain user ID, playable asset ID, current position, duration, and event time.
- Continue Watching must return unfinished content ordered by most recently watched.
- Mark content completed when playback reaches at least 90% of duration.
- Completed content must not appear in Continue Watching by default.

### FR-06: Watchlist (History Service)
- A viewer can add a title to a personal watchlist.
- A viewer can remove a title from a personal watchlist.
- Duplicate watchlist entries must be prevented.
- Watchlist entries must be returned newest-first.

### FR-07: Media Ingestion (Video-Service + Encoding-Service)
- Admin creates a title/episode in DRAFT state in Content-Service.
- Admin requests a pre-signed upload URL from Video-Service.
- Video-Service creates an upload intent and returns a pre-signed S3 URL.
- Source media uploads directly to S3 using the pre-signed URL.
- Upload completion creates a `video.uploaded` Kafka event.
- Encoding-Service consumes `video.uploaded`, downloads the raw file from S3, and transcodes it into HLS variants using FFmpeg.
- Encoding-Service produces an HLS master manifest and variant playlists and uploads them to S3.
- Encoding-Service updates processing state: QUEUED, PROCESSING, READY, FAILED.
- On success, Encoding-Service publishes a `video.encoded` Kafka event with the S3 master playlist key.
- A title can only be published when its required playable assets are READY.

### FR-08: Recommendation
- Phase 1: display deterministic content rails:
  - Trending.
  - Because You Watched [genre-based].
  - Similar Titles.
  - Recently Added.
- Phase 2: consume viewing events and calculate popularity and affinity scores.
- Recommendation failure must not block home-page rendering.

### FR-09: Subscription
- Phase 1 supports plans: FREE, BASIC, PREMIUM.
- Use a mock entitlement state; do not integrate a payment provider initially.
- Streaming-Service enforces entitlement rules.
- Payment provider and webhook processing are a Phase 3 enhancement.

### FR-10: Observability
- Every service exposes health and metrics endpoints.
- Every external request must include or generate a correlation ID.
- Distributed trace propagation must work through gateway, downstream services, and asynchronous events.
- Capture metrics for HTTP latency, errors, database connections, cache use, playback-session creation, media jobs, and event-consumer lag.
- Structured JSON logs must include timestamp, service name, trace ID, request ID, severity, and message.

## 6. Non-Functional Requirements

### Performance
- P95 API latency target for cached read APIs: under 300 ms in local/load-test conditions.
- Playback authorization endpoint target: P95 under 500 ms excluding external storage/CDN latency.
- Home page must use aggregated/cached data; do not make one browser request per content rail.
- No N+1 database queries in catalog listing or title-detail flows.

### Reliability
- All service-to-service calls must have explicit connection and read timeouts.
- Retry only idempotent transient operations.
- Use circuit breakers for remote dependencies where appropriate.
- Kafka consumers must be idempotent.
- Use a dead-letter strategy for repeatedly failing events.
- Use the transactional outbox pattern for reliable event publication.

### Security
- Never expose internal service URLs to the browser.
- Never return object-store credentials to clients.
- Pre-signed S3 URLs must have short expiry.
- Validate all DTO input.
- Enforce CORS at the API gateway.
- Store secrets only in environment variables or secret-management tooling.
- Do not log tokens, passwords, refresh tokens, or signed URLs.

### Testing
- Unit-test domain logic and controllers/services.
- Use Testcontainers for PostgreSQL, Redis, and Kafka integration tests.
- Add contract/API tests for gateway-facing endpoints.
- Add Playwright end-to-end tests for critical web flows.
- Add k6 or Gatling load tests for browse, search, playback authorization, and progress updates.

## 7. Primary User Flows

### Viewer playback flow
1. Viewer logs in.
2. Viewer browses or searches a published title.
3. Viewer opens the title detail page.
4. Viewer clicks Play.
5. Frontend requests a playback session through the API Gateway.
6. Streaming-Service validates authentication, title state, and entitlement.
7. Streaming-Service returns a short-lived pre-signed S3 HLS URL and session ID.
8. Browser player retrieves HLS playlist and segments directly from S3/CDN.
9. Browser posts periodic progress heartbeat events.
10. User returns later and uses Continue Watching.

### Admin ingestion flow
1. Admin creates a title or episode in DRAFT state in Content-Service.
2. Admin requests a pre-signed upload URL from Video-Service.
3. Video-Service creates an upload intent and returns a pre-signed S3 URL.
4. Source media uploads directly to S3.
5. Video-Service publishes a `video.uploaded` Kafka event.
6. Encoding-Service consumes the event, transcodes the video into HLS, and uploads outputs to S3.
7. Encoding-Service publishes a `video.encoded` Kafka event with the S3 master playlist key.
8. Streaming-Service consumes `video.encoded` and caches the master playlist key in Redis.
9. Admin confirms processing state is READY.
10. Admin publishes the title in Content-Service.
11. Content-Service emits `catalog.title.published.v1` event.
12. Search/recommendation consumers update their read models.

## 8. Acceptance Criteria: Phase 1

Phase 1 is complete only when:
- A user can register, log in, browse catalog data, and log out.
- An admin can create/publish a movie using seeded or fully encoded HLS media.
- A user can request playback and watch an HLS asset in the browser.
- Progress persists and Continue Watching works after refresh/re-login.
- Watchlist works.
- All browser traffic goes through the API gateway except direct signed media delivery from S3/CDN.
- Docker Compose starts the web app, gateway, core services, PostgreSQL, Redis, Kafka, MinIO (or S3 localstack), and observability tools.
- README includes architecture diagram, startup instructions, API documentation, and known limitations.
