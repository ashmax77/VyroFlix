# VyroFlix — Architecture (Final with Deployment)

## 1. Architecture Principles

- Use domain-oriented microservices, not one service per database table.
- Each service owns its database schema and data.
- Services communicate synchronously only when immediate response is required.
- Publish state changes asynchronously through Kafka using the transactional outbox pattern.
- Do not use distributed database transactions.
- The API Gateway is the only public backend entry point.
- Object storage (Cloudflare R2 / AWS S3) and CDN are the media delivery layer.
- Use REST/JSON for external API contracts and Kafka events for asynchronous integration.
- Design for deployment on free or minimal-cost tiers (Vercel, Render, Supabase, Upstash, Cloudflare).

## 2. High-Level Diagram

```text
                         +----------------------+
                         |  Next.js Web Client  |
                         | React + TypeScript   |
                         | Hosted on Vercel     |
                         +----------+-----------+
                                    |
                           HTTPS / REST / JSON
                                    |
                         +----------v-----------+
                         | Spring Cloud Gateway |
                         | Hosted on Render     |
                         | Auth, CORS, routing, |
                         | rate limiting        |
                         +----------+-----------+
                                    |
      +-----------------------------+------------------------------+
      |              |              |             |                |
+-----v-----+ +------v------+ +-----v-----+ +-----v------+ +------v------+
| Identity  | | Content     | | Streaming | | History    | | Search       |
| Service   | | Service     | | Service   | | Service    | | Service      |
+-----+-----+ +------+------| +-----+-----+ +-----+------+ +------+------+
      |              |              |             |                |
   Supabase       Supabase     Supabase+     Supabase       Supabase/
   Postgres       Postgres     Upstash     Postgres       OpenSearch
      |              |         Redis         |                |
      +--------------+--------------+-------------+----------------+
                                     |
                              +------v------+
                              | Upstash     |
                              | Kafka       |
                              +------+------+
                                     |
                    +----------------+----------------+
                    |                                 |
             +------v------+                   +------v-----------+
             | Video       |                   | Encoding         |
             | Service     |                   | Service          |
             +------+------+                   +------------------+
                    |                                 |
                    +--------------+------------------+
                                   |
                        +----------v-----------+
                        | Cloudflare R2        |
                        | (raw + HLS outputs)  |
                        +----------+-----------+
                                   |
                        +----------v-----------+
                        | Cloudflare CDN       |
                        +----------------------+
```

## 3. Services

### 3.1 API Gateway

Responsibilities:
- Single public API endpoint.
- Route requests to internal services.
- Apply CORS policy.
- Validate JWT access tokens issued by Supabase Auth.
- Enforce request-size limits and rate limits.
- Forward correlation/trace headers.
- Provide consistent error envelope.
- Expose API documentation aggregation if implemented.

Must not:
- Contain business logic.
- Access service databases.
- Proxy HLS segments or source video files.

Suggested routes:
- `/api/v1/auth/**` -> identity-service
- `/api/v1/users/**` -> identity-service
- `/api/v1/catalog/**` -> content-service
- `/api/v1/videos/**` -> video-service
- `/api/v1/encoding/**` -> encoding-service (admin/internal)
- `/api/v1/streaming/**` -> streaming-service
- `/api/v1/history/**` -> history-service
- `/api/v1/watchlist/**` -> history-service
- `/api/v1/search/**` -> search-service
- `/api/v1/admin/**` -> content-service, video-service, or encoding-service based on resource

Deployment: Spring Boot service on Render Free (or local for zero cost).

### 3.2 Identity Service

Responsibilities:
- Integrate with Supabase Auth for user registration and login (email/password and Google Sign-In).
- Map Supabase user IDs to internal user profiles and roles.
- Enforce role-based access control (USER, ADMIN).
- Optionally issue internal service-to-service tokens.

Database:
- Supabase PostgreSQL database (shared with other services or separate schema).

Core entities:
- User (mirrors Supabase auth.users)
- Role
- UserProfile
- DeviceSession

Events:
- `user.registered.v1`
- `user.profile.updated.v1`
- `user.deactivated.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

### 3.3 Content-Service (Catalog)

Responsibilities:
- Manage movies, series, seasons, episodes, genres, people, and artwork.
- Separate public read endpoints from admin write endpoints.
- Maintain publication state (DRAFT, PUBLISHED, UNPUBLISHED).
- Return optimized home-page rails and title detail projections.
- Validate title readiness before publishing (required media assets must be READY).
- Emit catalog publication events.

Database:
- Supabase PostgreSQL database.

Core entities:
- Title
- Season
- Episode
- Genre
- Person
- TitleCredit
- MediaAssetReference
- Collection
- PublicationState

Events:
- `catalog.title.created.v1`
- `catalog.title.updated.v1`
- `catalog.title.published.v1`
- `catalog.title.unpublished.v1`
- `catalog.episode.published.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

### 3.4 Video-Service (Uploads)

Responsibilities:
- Create upload intents for titles/episodes.
- Generate pre-signed Cloudflare R2 (or S3) URLs for direct client uploads.
- Track upload state (PENDING, UPLOADED, FAILED).
- On upload completion, publish `video.uploaded.v1` Kafka event with:
  - upload intent ID
  - R2/S3 bucket and key of raw file
  - associated content/asset ID
  - correlation ID
- Support multipart upload coordination if required.

Database:
- Supabase PostgreSQL for upload intents and metadata.

Storage:
- Cloudflare R2 (or AWS S3) for raw video files.

Core entities:
- UploadIntent
- UploadMetadata
- UploadStatus

Events produced:
- `video.uploaded.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

### 3.5 Encoding-Service (Processing)

Responsibilities:
- Consume `video.uploaded.v1` events from Upstash Kafka.
- Download raw video from R2/S3.
- Use FFmpeg to encode video into multiple HLS renditions (e.g., 1080p, 720p, 480p) with:
  - H.264 video, AAC audio.
  - Fixed GOP and aligned keyframes for smooth ABR switching.
  - 2–6 second segments for VOD.
- Generate:
  - Variant playlists (`playlist_1080p.m3u8`, etc.).
  - Master playlist (`master.m3u8`) referencing all variants.
- Upload HLS segments and playlists to R2/S3 under a structured key, e.g.:

  ```text
  r2://vyroflix-content/{contentId}/{assetId}/hls/
    master.m3u8
    playlist_1080p.m3u8
    playlist_720p.m3u8
    playlist_480p.m3u8
    segments/...
  ```

- Persist encoding job metadata (status, R2/S3 keys, renditions, duration).
- On success:
  - Mark job READY.
  - Publish `video.encoded.v1` event with:
    - content/asset IDs
    - R2/S3 master playlist key
    - duration, renditions metadata
- On failure:
  - Mark job FAILED, preserve diagnostics, allow retry.

Database:
- Supabase PostgreSQL for encoding jobs and asset metadata.

Storage:
- Cloudflare R2 (or AWS S3) for HLS outputs.

Core entities:
- EncodingJob
- HlsAsset
- Rendition
- EncodingStatus

Events produced:
- `video.encoded.v1`

Deployment: Can run as a separate service on Render Free, Railway, or locally (recommended to run locally for cost savings in early phases).

### 3.6 Streaming-Service (Playback)

Responsibilities:
- Consume `video.encoded.v1` events.
- For each encoded asset:
  - Store the master playlist R2/S3 key in Upstash Redis for fast lookup.
  - Optionally cache entitlement-related metadata.
- Create playback sessions:
  - Verify user (via Supabase JWT), content availability, and entitlement.
  - Generate short-lived pre-signed R2/S3 URLs to the HLS master playlist.
  - Record playback start and end metadata.
  - Apply basic concurrent-session limits by plan.
- Never transcode, serve, or proxy segments.

Database:
- Supabase PostgreSQL for sessions/audit records.

Cache:
- Upstash Redis for master playlist keys, hot entitlement data, and rate limits.

Core entities:
- PlaybackSession
- PlaybackAuthorization
- EntitlementSnapshot
- DevicePlaybackState

Events:
- `playback.started.v1`
- `playback.ended.v1`
- `playback.authorization.denied.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

### 3.7 History Service

Responsibilities:
- Persist progress heartbeats.
- Manage Continue Watching.
- Manage watchlist.
- Mark content completed.
- Produce user viewing events for recommendations and analytics.

Database:
- Supabase PostgreSQL for Phase 1.

Core entities:
- ViewingProgress
- WatchlistEntry
- CompletionRecord

Events:
- `history.progress.updated.v1`
- `history.content.completed.v1`
- `watchlist.item.added.v1`
- `watchlist.item.removed.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

### 3.8 Search Service

Responsibilities:
- Consume catalog publication events.
- Maintain a denormalized search index.
- Search/filter/sort titles.
- Return lightweight search cards.

Database:
- Phase 1: Supabase PostgreSQL full-text index.
- Phase 2: OpenSearch (self-hosted or managed) if needed.

Events consumed:
- `catalog.title.published.v1`
- `catalog.title.updated.v1`
- `catalog.title.unpublished.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

### 3.9 Recommendation Service

Responsibilities:
- Build deterministic and event-driven recommendation rails.
- Compute trending scores.
- Provide similar-content results.
- Cache recommendation responses.
- Fail gracefully: homepage remains usable if unavailable.

Storage:
- Upstash Redis cache.
- Supabase PostgreSQL/materialized views initially.

Events consumed:
- `playback.started.v1`
- `history.progress.updated.v1`
- `history.content.completed.v1`
- `catalog.title.published.v1`

Deployment: Part of the main Spring Boot application on Render Free or local.

## 4. Event Standards

Every event must include:

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

Rules:
- Use immutable versioned event types.
- Consumers must be idempotent using `eventId`.
- Never put passwords, JWTs, signed URLs, or raw payment data in events.
- Use a dead-letter topic/queue after configured retry attempts.
- Keep event payloads intentionally small.
- Use the transactional outbox pattern for reliable event publication:
  - Services write events to an outbox table in the same DB transaction as business data.
  - A relay process publishes from outbox to Upstash Kafka and marks records as sent.

## 5. Data Ownership

- Supabase Auth owns user credentials and identity providers (Google, email/password).
- Identity Service owns internal user profiles and roles.
- Content-Service owns content metadata and publication state.
- Video-Service owns upload intents and raw-upload metadata.
- Encoding-Service owns encoding jobs and HLS asset metadata.
- Streaming-Service owns playback-session audit data.
- History-Service owns viewing-progress and watchlist data.
- Search-Service owns search index/read model.
- Recommendation-Service owns recommendation cache/read models.

No service may read another service's database directly.

## 6. Key API Contracts

### Request upload URL (Video-Service)

```http
POST /api/v1/videos/upload-intents
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "contentId": "uuid",
  "assetType": "MOVIE" | "EPISODE",
  "filename": "source.mp4",
  "contentType": "video/mp4"
}
```

Response:

```json
{
  "uploadIntentId": "uuid",
  "uploadUrl": "https://<r2-bucket>.r2.cloudflarestorage.com/...?X-Amz-...",
  "expiresAt": "2026-09-13T05:00:00Z"
}
```

### Create playback session (Streaming-Service)

```http
POST /api/v1/streaming/sessions
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "playableAssetId": "uuid",
  "deviceId": "browser-device-id"
}
```

Response:

```json
{
  "sessionId": "uuid",
  "manifestUrl": "https://<r2-bucket>.r2.cloudflarestorage.com/.../master.m3u8?X-Amz-...",
  "expiresAt": "2026-09-13T05:10:00Z",
  "resumePositionSeconds": 125
}
```

### Save playback progress (History Service)

```http
PUT /api/v1/history/progress
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "playableAssetId": "uuid",
  "positionSeconds": 180,
  "durationSeconds": 3600,
  "playbackSessionId": "uuid"
}
```

### Search catalog (Search Service)

```http
GET /api/v1/search?q=space&type=MOVIE&genre=SCI_FI&page=0&size=24
Authorization: Bearer <access-token>
```

## 7. Media Delivery Model

1. Browser asks Streaming-Service for authorization.
2. Streaming-Service returns a short-lived pre-signed R2/S3 URL to the HLS master playlist.
3. Browser player fetches master playlist and segments from R2/S3 + Cloudflare CDN.
4. CDN handles repeated segment delivery and geographical scaling.
5. Browser sends progress to History Service via API Gateway.
6. Video bytes must never travel through API Gateway or Spring services.

## 8. Failure Behaviour

- Content-Service unavailable: show cached home rails if available; show a friendly retry state.
- Recommendation unavailable: omit recommendation row; do not fail the home page.
- History unavailable: allow playback; retry progress asynchronously.
- Streaming-Service unavailable: player must not start; show actionable error.
- Encoding job failure: mark asset FAILED, preserve diagnostics, permit retry.
- Kafka failure: use outbox/retry mechanisms; core synchronous reads remain available where possible.

## 9. Deployment & Cost Strategy

- **Frontend:** Next.js on Vercel Hobby (free).
- **Backend:** Spring Boot services on Render Free (or local for zero cost).
- **PostgreSQL:** Supabase Free (500 MB per project).
- **Redis:** Upstash Redis Free (256 MB, 500K commands/month).
- **Kafka:** Upstash Kafka Free (~10K messages/day).
- **Object Storage + CDN:** Cloudflare R2 + Cloudflare CDN (no egress fees within allowance).
- **Auth:** Supabase Auth.

### 9.1 Frontend (Vercel Hobby)

- Host the Next.js application on Vercel Hobby.
- Use environment variable `NEXT_PUBLIC_API_BASE_URL` to point to the Render-hosted API Gateway.
- Benefits:
  - Free global CDN for static and SSR content.
  - 100 GB bandwidth/month and 1M serverless function invocations/month (Hobby limits).
  - Simple Git-based deployments.
- Constraints:
  - Personal/non-commercial use only on Hobby plan.
  - Serverless functions limited to 10s execution time; keep business logic in Spring Boot.

### 9.2 Backend (Render Free)

- Deploy Spring Boot services to Render Free web services.
- For cost efficiency, initially combine multiple logical services into one Spring Boot application:
  - API Gateway
  - Identity Service
  - Content-Service
  - Streaming-Service
  - History Service
  - Search Service
  - Recommendation Service
- Run Video-Service and Encoding-Service locally or as a separate low-usage service.
- Benefits:
  - Free tier available (with sleep after inactivity).
  - Docker-based deployments.
- Constraints:
  - Cold starts (~30–60s) after inactivity.
  - Limited build minutes and CPU.
- Alternative:
  - Railway (~$5/month) for better uptime and performance if needed.

### 9.3 Database (Supabase Free)

- Use a single Supabase project for all PostgreSQL needs.
- Create separate schemas per service (e.g., `identity`, `content`, `video`, `encoding`, `streaming`, `history`).
- Benefits:
  - 500 MB database storage free.
  - 50K monthly active users for Auth.
  - 5 GB egress/month.
- Constraints:
  - Projects may pause after 1 week of inactivity on free tier.
  - Not suitable for large binary data (store only metadata).

### 9.4 Redis (Upstash Redis Free)

- Use Upstash Redis for:
  - Caching master playlist keys.
  - Short-lived session/entitlement data.
  - Rate limiting keys.
- Benefits:
  - 256 MB data storage.
  - 500K commands/month.
  - 10 GB bandwidth/month.
- Constraints:
  - Sufficient for portfolio-scale traffic; monitor usage.

### 9.5 Kafka (Upstash Kafka Free)

- Use Upstash Kafka as the event bus.
- Topics include:
  - `video.uploaded.v1`
  - `video.encoded.v1`
  - `catalog.title.published.v1`
  - `history.progress.updated.v1`
- Benefits:
  - ~10K messages/day on free tier.
  - Serverless, HTTP-based access.
- Constraints:
  - Adequate for development and moderate demo traffic.
  - For local development, Kafka can also run in Docker.

### 9.6 Object Storage + CDN (Cloudflare R2 + Cloudflare CDN)

- Use Cloudflare R2 for:
  - Raw video uploads.
  - HLS segments and playlists.
- Put Cloudflare CDN in front for:
  - Edge caching of HLS segments.
  - Low-latency global delivery.
- Benefits:
  - S3-compatible API.
  - No egress fees within free/low-cost allowances.
  - Very low storage cost.
- Alternative:
  - AWS S3 (5 GB free for 12 months) + CloudFront (1 TB/month data transfer out free, always-free).

### 9.7 Authentication (Supabase Auth)

- Use Supabase Auth for:
  - Email/password authentication.
  - Google Sign-In (OAuth).
- Configure Google OAuth in:
  - Google Cloud Console (OAuth client ID and secret).
  - Supabase dashboard (Authentication → Providers → Google).
- Benefits:
  - 50K monthly active users free.
  - JWT tokens verifiable by Spring Cloud Gateway.
- Constraints:
  - Ensure redirect URIs are correctly configured for both local and production environments.

### 9.8 Approximate Monthly Cost

| Component | Platform | Monthly Cost (Approx.) |
|---|---|---|
| Frontend | Vercel Hobby | $0 |
| Backend | Render Free | $0 (with sleep) or ~$5–7 on Railway |
| PostgreSQL + Auth | Supabase Free | $0 |
| Redis | Upstash Redis Free | $0 |
| Kafka | Upstash Kafka Free | $0 |
| Storage + CDN | Cloudflare R2 + CDN | $0–5 (depending on usage) |
| **Total** | | **$0–10/month** |

This deployment strategy keeps VyroFlix within free or minimal-cost tiers while preserving a realistic, production-like microservice architecture suitable for a portfolio project.
