# VyroFlix API Reference

All browser traffic goes through the API Gateway, or through the combined `platform` service in the free-tier deployment (Vercel rewrites `/api/v1/**` to Render). Public routes live under `/api/v1`. Authenticated routes expect a Supabase-issued JWT in `Authorization: Bearer <token>`.

The controllers are authoritative. This document is derived from `services/*/src/main/java/**/controller`.

**Identity comes only from the token.** No endpoint trusts a client-supplied user id (`X-User-Id` is ignored). Roles and plans are read from the token's server-controlled `app_metadata` (`role`: `USER` or `ADMIN`; `plan`: `FREE`, `BASIC` or `PREMIUM`). `user_metadata` is never used.

## Errors

Errors use one envelope:

```json
{ "timestamp": "2026-10-28T10:00:00Z", "status": 404, "code": "TITLE_NOT_FOUND",
  "message": "Title is not available for playback", "path": "/api/v1/streaming/sessions",
  "correlationId": "uuid" }
```

| HTTP | `code` examples | Meaning |
|---|---|---|
| 401 | `UNAUTHORIZED` | No valid token. |
| 403 | `FORBIDDEN` | Signed in, but not allowed (wrong role or not the owner). |
| 404 | `TITLE_NOT_FOUND`, `SESSION_NOT_FOUND`, `NOT_FOUND` | Missing, unpublished, or not yours. Unknown and unpublished titles return the same response. |
| 409 | `CONFLICT` | Duplicate slug, or deleting a published title. |
| 422 | `VALIDATION_FAILED`, `ASSET_NOT_READY` | Invalid input, or media not encoded yet. |
| 429 | `CONCURRENT_STREAM_LIMIT` | The plan's concurrent-stream limit is reached. End another stream. |

Every response carries an `X-Correlation-Id` header. Stack traces are never returned.

## Identity: `/api/v1/auth`, `/api/v1/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/sync` | Authenticated | Create or refresh the profile. `body.userId` must equal the token's subject (else 403). Emits `user.registered.v1` on first sync. |
| GET | `/api/v1/users/me` | Authenticated | Current profile. |
| PUT | `/api/v1/users/me` | Authenticated | Update the current profile. |
| GET | `/api/v1/users/{id}` | Self or `ADMIN` | Profile by id. Other users get 403. |

## Catalog: `/api/v1/catalog`

| Method | Path | Query | Auth | Description |
|---|---|---|---|---|
| GET | `/api/v1/catalog/home` | — | Public | Home feed (rails). |
| GET | `/api/v1/catalog/titles` | `type` (`MOVIE`/`SERIES`), `genre` (slug) | Public | Published titles. |
| GET | `/api/v1/catalog/titles/{id}` | — | Public | Title details, including `cast` (`[{personId, name, avatarUrl, role, characterName}]`). `cast` is absent from list responses. |
| GET | `/api/v1/catalog/collections` | — | Public | Curated collections with their published titles. Empty collections are omitted. |
| GET | `/api/v1/catalog/genres` | — | Public | All genres. |

## Admin catalog: `/api/v1/admin` (`ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/admin/titles` | Create a title in `DRAFT`. |
| GET | `/api/v1/admin/titles` | All titles in every state. Optional `state`, `type`. |
| GET | `/api/v1/admin/titles/{id}` | One title with admin metadata. |
| PUT | `/api/v1/admin/titles/{id}` | Update metadata. Updating a published title emits `catalog.title.updated.v1`. |
| DELETE | `/api/v1/admin/titles/{id}` | Delete. **204**, or **409** when the title is published. Seasons, episodes, credits and collection links are removed with it. |
| POST | `/api/v1/admin/titles/{id}/publish` | Publish. Requires READY media (movie) or at least one READY episode (series). Emits `catalog.title.published.v1` and `catalog.episode.published.v1` for each episode. |
| POST | `/api/v1/admin/titles/{id}/unpublish` | Unpublish. Emits `catalog.title.unpublished.v1`. |
| POST | `/api/v1/admin/titles/{id}/seasons` | Add a season to a series. |
| POST | `/api/v1/admin/seasons/{seasonId}/episodes` | Add an episode. |
| POST | `/api/v1/admin/persons` | Create a person (`{ "name": "...", "avatarUrl": "..." }`). |
| PUT | `/api/v1/admin/titles/{id}/credits` | Replace the full credit list: `{ "credits": [{ "personId", "role": "ACTOR\|DIRECTOR\|WRITER\|PRODUCER", "characterName" }] }`. Unknown people are rejected. Returns the cast. |
| GET | `/api/v1/admin/collections` | All collections with every title and its state. |
| POST | `/api/v1/admin/collections` | Create a collection (`{ "name", "slug", "displayOrder" }`). Slugs are lowercase with hyphens. |
| PUT | `/api/v1/admin/collections/{id}/titles` | Replace the titles: `{ "titleIds": [...] }`. Every id must exist. |
| GET / POST | `/api/v1/admin/genres` | List or create genres. |
| GET | `/api/v1/admin/stats/catalog` | Counts by type and publication state. |

## Uploads: `/api/v1/videos/upload-intents` (`ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/videos/upload-intents` | Create an upload intent; returns a short-lived pre-signed PUT URL. The browser uploads **directly** to R2. |
| PUT | `/api/v1/videos/upload-intents/{id}/complete` | Verify the object exists and emit `video.uploaded.v1`. |
| GET | `/api/v1/videos/upload-intents/{id}` | Intent status. |
| GET | `/api/v1/videos/upload-intents/content/{contentId}` | Intents for a title. |

## Encoding: `/api/v1/encoding` (`ADMIN`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/encoding/jobs` | List jobs (`QUEUED`, `PROCESSING`, `READY`, `FAILED`). |
| GET | `/api/v1/encoding/jobs/{id}` | Job details. |
| GET | `/api/v1/encoding/jobs/content/{contentId}` | Jobs for a title. |
| POST | `/api/v1/encoding/jobs/{id}/retry` | Retry a failed job. |
| GET | `/api/v1/encoding/stats` | Job counts by status. |

The encoding worker runs outside the combined Render app. When it is offline these routes can return 404 or 502 and the admin page shows "encoding service not reachable".

## Playback: `/api/v1/streaming` (authenticated)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/streaming/sessions` | Authorise playback. Returns `201`. |
| POST | `/api/v1/streaming/sessions/{sessionId}/end` | End your own session. **204**, idempotent. Frees a stream slot. |

Request:

```json
{ "contentId": "uuid", "targetType": "MOVIE", "deviceId": "browser-device-id" }
```

`targetType` is `MOVIE` or `EPISODE`. For an episode, `contentId` is the episode id; the episode and its series must both be published.

Response:

```json
{ "sessionId": "uuid", "manifestUrl": "https://media.example.com/titles/{id}/hls/master.m3u8?exp=...&sig=...",
  "expiresAt": "2026-10-28T10:15:00Z", "resumePositionSeconds": 125 }
```

- `manifestUrl` is valid for 15 minutes. The Worker issues longer-lived child tokens for variants and segments.
- `resumePositionSeconds` comes from your saved progress (0 when none, or when History is unavailable).
- The URL is never logged and no storage credentials are returned.
- Concurrent streams per plan: `FREE` 1, `BASIC` 2, `PREMIUM` 4. Exceeding it returns 429.

## History: `/api/v1/history`

| Method | Path | Query | Description |
|---|---|---|---|
| PUT | `/api/v1/history/progress` | — | Idempotent progress upsert: `{ "contentId", "targetType", "positionSeconds", "durationSeconds" }`. |
| GET | `/api/v1/history/progress/{contentId}` | — | Your saved progress for one title. 404 when none. |
| GET | `/api/v1/history/continue-watching` | `limit` (20, max 50) | Unfinished titles, most recent first. |
| GET | `/api/v1/history/watchlist` | `page`, `size` | Watchlist, newest first. |
| POST | `/api/v1/history/watchlist` | — | Add `{ "titleId" }`. Duplicates are rejected. |
| DELETE | `/api/v1/history/watchlist/{titleId}` | — | Remove a title. |
| GET | `/api/v1/history/watchlist/{titleId}/check` | — | Whether the title is on the watchlist. |

A title is complete at 90% of its duration; completed titles leave Continue Watching.

## Search: `/api/v1/search`

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/api/v1/search` | `q`, `type`, `genre` (slug), `year`, `maturity`, `page` (0), `size` (20, max 50) | Full-text search with optional filters over published titles. Cast and directors are searchable. `%` and `_` match literally. |
| GET | `/api/v1/search/suggestions` | `q` (required), `limit` (10) | Title prefix suggestions. |

## Recommendations: `/api/v1/recommendations`

| Method | Path | Query | Auth | Description |
|---|---|---|---|---|
| GET | `/api/v1/recommendations/trending` | `limit` (10) | Public | Trending rail. |
| GET | `/api/v1/recommendations/similar/{titleId}` | `limit` (10) | Public | Similar titles. |
| GET | `/api/v1/recommendations/personal` | `limit` (10) | Authenticated | Personal rail. Fails open to an empty list on error. |

## Admin statistics: `/api/v1/admin/stats` (`ADMIN`)

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/api/v1/admin/stats/catalog` | — | Title counts (see Admin catalog). |
| GET | `/api/v1/admin/stats/playback` | `days` (7, max 90) | `activeSessions` now and `daily` session counts. Aggregates only. |

Each figure comes from the service that owns it. No endpoint returns per-user analytics.

## Operations

- `/actuator/health`: liveness and readiness probes. Details are shown only to authorised callers.
- `/actuator/prometheus`: Prometheus scrape endpoint on every service.
- `/v3/api-docs` and `/swagger-ui.html`: OpenAPI description and UI (public, describes the contract only).

## Security summary

| Rule | Enforced by |
|---|---|
| `/api/v1/admin/**`, `/api/v1/videos/**`, `/api/v1/encoding/**` need `ADMIN` | `JwtSecurityConfig` (path rules) |
| Catalog reads, search, and trending/similar recommendations are public | `JwtSecurityConfig` |
| Everything else under `/api/v1` needs a valid token | `JwtSecurityConfig` |
| Owner-only resources (sessions, progress, watchlist, profile) | Service code, using the token's subject |
| CORS allows only configured origins | `vyroflix.security.cors-allowed-origins` / `CORS_ALLOWED_ORIGINS` |
