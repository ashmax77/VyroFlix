# VyroFlix API Reference

All browser traffic goes through the API Gateway (or the combined `platform` service in the
free-tier deployment). Public routes live under `/api/v1`. Authenticated routes expect a
Supabase-issued JWT in `Authorization: Bearer <token>`.

> This reference is derived from the controllers in `services/*/src/main/java/**/controller`.
> Where it differs from the illustrative examples in [`../ARCHITECTURE.md`](../ARCHITECTURE.md),
> the controllers are authoritative (see [Known differences](#known-differences)).

Errors use the shared `ErrorResponse` envelope from `common`
(`timestamp`, `status`, `code`, `message`, `path`, optional `correlationId` / `details`).
Every response carries an `X-Correlation-Id` header.

## Identity — `/api/v1/auth`, `/api/v1/users`

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/auth/sync` | Create/refresh the internal profile for the authenticated Supabase user. Emits `user.registered.v1` on first sync. |
| GET | `/api/v1/users/me` | Current user's profile. |
| PUT | `/api/v1/users/me` | Update the current user's profile. |
| GET | `/api/v1/users/{id}` | Fetch a profile by id. |

## Catalog — `/api/v1/catalog` (Content-Service)

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/api/v1/catalog/home` | — | Home feed (rails). |
| GET | `/api/v1/catalog/titles` | `type` (`MOVIE`/`SERIES`), `genre` | Published titles, optionally filtered. |
| GET | `/api/v1/catalog/titles/{id}` | — | Title details. |
| GET | `/api/v1/catalog/genres` | — | All genres. |

## Admin catalog — `/api/v1/admin` (role `ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/admin/titles` | Create a title in `DRAFT`. |
| GET | `/api/v1/admin/titles` | List titles (all publication states). |
| GET | `/api/v1/admin/titles/{id}` | Title with admin metadata. |
| PUT | `/api/v1/admin/titles/{id}` | Update a title. |
| POST | `/api/v1/admin/titles/{id}/publish` | Publish (requires a `READY` media asset). Emits `catalog.title.published.v1`. |
| POST | `/api/v1/admin/titles/{id}/unpublish` | Unpublish. Emits `catalog.title.unpublished.v1`. |
| POST | `/api/v1/admin/titles/{id}/seasons` | Add a season to a series. |
| POST | `/api/v1/admin/seasons/{seasonId}/episodes` | Add an episode. |
| GET / POST | `/api/v1/admin/genres` | List / create genres. |

## Uploads — `/api/v1/videos/upload-intents` (Video-Service)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/videos/upload-intents` | Create an upload intent; returns a short-lived pre-signed PUT URL. The browser uploads **directly** to R2/MinIO. |
| PUT | `/api/v1/videos/upload-intents/{id}/complete` | Verify the object exists and emit `video.uploaded.v1`. |
| GET | `/api/v1/videos/upload-intents/{id}` | Upload intent status. |
| GET | `/api/v1/videos/upload-intents/content/{contentId}` | Upload intents for a title. |

## Encoding jobs — `/api/v1/encoding/jobs` (Encoding-Service)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/encoding/jobs` | List jobs (`QUEUED`, `PROCESSING`, `READY`, `FAILED`). |
| GET | `/api/v1/encoding/jobs/{id}` | Job details. |
| GET | `/api/v1/encoding/jobs/content/{contentId}` | Jobs for a title. |
| POST | `/api/v1/encoding/jobs/{id}/retry` | Retry a failed job. |

## Playback — `/api/v1/streaming` (Streaming-Service)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/streaming/sessions` | Create a playback session. Returns `201` with a short-lived signed `manifestUrl`. |

Request body:

```json
{ "contentId": "uuid", "targetType": "MOVIE", "deviceId": "browser-device-id" }
```

`contentId` is required. The response contains the session id, the short-lived `manifestUrl`,
`expiresAt`, and `resumePositionSeconds`. The URL is never logged and no storage credentials
are returned. Video bytes are served by R2/CDN, never by this API.

## History — `/api/v1/history` (History-Service)

| Method | Path | Query | Description |
|---|---|---|---|
| PUT | `/api/v1/history/progress` | — | Idempotent progress upsert. |
| GET | `/api/v1/history/continue-watching` | `limit` (default 20) | Continue-Watching rail. |
| GET | `/api/v1/history/watchlist` | `page` (0), `size` (24) | Paginated watchlist. |
| POST | `/api/v1/history/watchlist` | — | Add a title (`{ "titleId": "uuid" }`). |
| DELETE | `/api/v1/history/watchlist/{titleId}` | — | Remove a title. |
| GET | `/api/v1/history/watchlist/{titleId}/check` | — | Is the title on the watchlist? |

Progress body (`contentId`, `targetType`, `positionSeconds >= 0`, `durationSeconds >= 1`):

```json
{ "contentId": "uuid", "targetType": "MOVIE", "positionSeconds": 180, "durationSeconds": 3600 }
```

A title is marked complete at the configured threshold (normally 90%).

## Search — `/api/v1/search` (Search-Service)

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/api/v1/search` | `q`, `type`, `page` (0), `size` (20) | PostgreSQL full-text search over the denormalized index. |
| GET | `/api/v1/search/suggestions` | `q` (required prefix), `limit` (10) | Autocomplete suggestions. |

## Recommendations — `/api/v1/recommendations` (Recommendation-Service)

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/api/v1/recommendations/trending` | `limit` (10) | Trending rail. |
| GET | `/api/v1/recommendations/similar/{titleId}` | `limit` (10) | Genre-similar titles. |
| GET | `/api/v1/recommendations/personal` | `limit` (10) | Personalized rail (fails open). |

## Health and metrics

Every service exposes `/actuator/health` (with liveness/readiness probes) and
`/actuator/prometheus`.

## Known differences

- `ARCHITECTURE.md` shows the session body as `playableAssetId`; the implemented field is `contentId`.
- `ARCHITECTURE.md` shows a `genre` filter on search; `SearchController` currently accepts `q` and `type` only.
- The streaming and history controllers accept an `X-User-Id` header as a fallback when no authenticated
  principal is present. That header is only safe behind the gateway; see the security notes in
  `AGENTS.md` ("do not trust an arbitrary forwarded user ID header").
- Role rules are enforced in `JwtSecurityConfig`: `/api/v1/admin/**` and `POST /api/v1/videos/upload-intents`
  require `ADMIN`.
