# VyroFlix — Zero-Cost Cloud Deployment Guide

This guide deploys VyroFlix within free-tier quotas. It is a portfolio and demo environment, not a high-availability service. Quotas change; check each provider before relying on them.

Order matters. Complete each step before the next, and run the checks in [§8](#8-verify-the-live-deployment).

---

## 1. Architecture

| Component | Provider | Holds |
|---|---|---|
| Frontend | Vercel Hobby | Next.js app. Rewrites `/api/v1/**` to the backend. |
| Backend | Render Free | Combined Spring Boot platform (all domain modules except encoding) |
| Database & sign-in | Supabase Free | Per-service schemas; Auth (email and Google) |
| Cache | Upstash Redis | Master-playlist key cache |
| Events | Upstash Kafka | Versioned domain events (`docs/events/EVENT_CATALOG.md`) |
| Media | Cloudflare R2 (private) | Raw uploads and encoded HLS |
| Media delivery | Cloudflare Worker | Token check, playlist rewrite, segment serving (`infra/cloudflare/hls-worker`) |
| Encoding | Your machine (or a separate host) | FFmpeg worker. Too CPU-heavy for the free tier. |

No video bytes pass through Render or Vercel.

---

## 2. Supabase (database and sign-in)

1. Create a project named `vyroflix` and note the database password.
2. **SQL Editor:** run [`infra/supabase/01-init-supabase-schemas.sql`](../infra/supabase/01-init-supabase-schemas.sql). It creates the service schemas and sets the search path the outbox depends on.
3. **Connection string:** use the **Session pooler** string (IPv4-compatible). The direct `db.<ref>.supabase.co` host is IPv6-only.
   ```text
   jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require
   ```
4. **Google sign-in:** create a Google OAuth client, add the Supabase callback URL to it, then enable Google under *Authentication → Providers*. Add `https://<your-vercel-domain>/auth/callback` and `http://localhost:3000/auth/callback` under *Authentication → URL Configuration*.
5. **Admin account.** Roles come only from the token's `app_metadata`. `user_metadata` is editable by users and is never trusted. Admin is restricted to one account: the JWT converter honours `ADMIN` only for emails in `Constants.ADMIN_EMAIL_ALLOWLIST` (currently `imax77work@gmail.com`). Every other account is a USER with watch access only. To grant the role, run this, then have that account sign out and back in:
   ```sql
   update auth.users
   set raw_app_meta_data = coalesce(raw_app_meta_data, '{}'::jsonb) || '{"role": "ADMIN"}'::jsonb
   where email = 'imax77work@gmail.com';
   ```
   To hand admin to someone else, change the allowlist, redeploy, then grant the role to their account.
6. **Subscription plans** (manual, until payments exist). Valid values are `FREE` (the default), `BASIC` and `PREMIUM`:
   ```sql
   update auth.users
   set raw_app_meta_data = coalesce(raw_app_meta_data, '{}'::jsonb) || '{"plan": "PREMIUM"}'::jsonb
   where email = 'viewer@example.com';
   ```
   Changes take effect at the next sign-in, because the plan is read from the access token.
7. **API keys:** copy the Project URL and the `anon` public key for the frontend. Never put the `service_role` key in the frontend or in the repository.

---

## 3. Upstash (Redis and Kafka)

1. **Redis:** create `vyroflix-redis` with TLS enabled and `volatile-lru` eviction. Copy the `rediss://` URL.
2. **Kafka:** create `vyroflix-kafka` (serverless). Create these topics and their dead-letter topics. Managed clusters do not auto-create them.

   | Topic | Consumed by |
   |---|---|
   | `video.uploaded.v1` | Encoding worker |
   | `video.encoded.v1` | Streaming, Content |
   | `catalog.title.published.v1` | Search, Recommendation, Streaming |
   | `catalog.title.unpublished.v1` | Search, Streaming |
   | `catalog.title.updated.v1` | Search |
   | `catalog.episode.published.v1` | Streaming |
   | `playback.started.v1` | Recommendation |
   | `playback.ended.v1` | (none yet) |
   | `history.content.completed.v1` | Recommendation |
   | `user.registered.v1` | (none yet) |
   | `watchlist.item.added.v1`, `watchlist.item.removed.v1` | (none yet) |

   Dead-letter topics: append `.DLT` to each consumed topic above (for example `video.encoded.v1.DLT`). Create them explicitly, because a managed broker may not auto-create them. Confirm the names match what Spring publishes after a forced failure.

3. Copy the bootstrap server and SASL credentials (SCRAM-SHA-256). Store them only in Render's environment variables.

---

## 4. Cloudflare R2 and the media Worker

### 4.1 Buckets

| Bucket | Access | Purpose |
|---|---|---|
| `vyroflix-raw` | Private | Source uploads (browser PUTs via pre-signed URLs) |
| `vyroflix-content` | **Private. No public access and no `r2.dev` domain.** | Encoded HLS. Only the Worker reads it. |

### 4.2 CORS on the raw bucket

Browsers upload straight to R2, so the raw bucket must allow the upload origin. Set this in *R2 → vyroflix-raw → Settings → CORS*:

```json
[
  {
    "AllowedOrigins": ["https://YOUR-APP.vercel.app", "http://localhost:3000"],
    "AllowedMethods": ["PUT", "HEAD"],
    "AllowedHeaders": ["Content-Type"],
    "MaxAgeSeconds": 3600
  }
]
```

The content bucket needs no browser CORS: the Worker sends the CORS headers itself.

### 4.3 API token

Create an R2 API token with *Object Read & Write* on both buckets only. Keep the Access Key ID, Secret, and endpoint (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com`) for Render.

### 4.4 Deploy the Worker

The Worker validates the playback token that Render issues and serves HLS from the private content bucket.

1. Pick a hostname for media, for example `media.example.com`. It must be on a Cloudflare zone.
2. Edit [`infra/cloudflare/hls-worker/wrangler.toml`](../infra/cloudflare/hls-worker/wrangler.toml): set the `routes` pattern to your hostname.
3. Generate one shared secret (at least 32 characters, for example `openssl rand -base64 48`). Use the same value in both places below.
   ```bash
   cd infra/cloudflare/hls-worker
   npx wrangler secret put HLS_SIGNING_SECRET     # paste the secret
   npx wrangler deploy
   ```
4. Run the Worker's unit tests (no Cloudflare account needed): `node --test infra/cloudflare/hls-worker/test/worker.test.mjs`.
5. Check the route: an unsigned request to `https://media.example.com/titles/x/hls/master.m3u8` must return 403.

**Known limits**
- Each child token lasts four hours (`CHILD_TOKEN_TTL_SECONDS`). Anyone holding a live token can fetch that title's segments until it expires.
- Segments are cached per token query string, so the cache is per session. If R2 egress becomes a concern, add a cache rule that ignores the query string for `.ts` and `.m4s` files.

---

## 5. Render (backend)

1. **New → Blueprint** from the repository. Render reads [`render.yaml`](../render.yaml).
2. Fill in the `sync: false` variables:

   | Variable | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Supabase session-pooler JDBC URL and credentials |
   | `SUPABASE_URL` | `https://<ref>.supabase.co` (derives the JWKS URI and issuer) |
   | `CORS_ALLOWED_ORIGINS` | Your exact Vercel origin, for example `https://vyroflix.vercel.app`. Add a custom domain if you use one. Do not use `*`. |
   | `UPSTASH_REDIS_URL` | `rediss://…` |
   | `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SASL_JAAS_CONFIG` | From Upstash |
   | `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | From §4.3 |
   | `PLAYBACK_CDN_BASE_URL` | `https://media.example.com` (the Worker route, no trailing slash) |
   | `PLAYBACK_SIGNING_SECRET` | The same secret as the Worker's `HLS_SIGNING_SECRET` |

3. Deploy, then open `https://<service>.onrender.com/actuator/health`. It should report `UP`.
4. **Memory:** the image caps the heap at 320 MB and bounds metaspace, the code cache, and thread stacks, to fit Render Free's 512 MB. After the first real traffic, check `/actuator/metrics/jvm.memory.used` (and the `area=nonheap` tag). If usage is close to 512 MB, reduce `-Xmx` before anything else.
5. **Logs** are JSON on this profile, one object per line with `correlationId`. Never add logging of headers, tokens or signed URLs.
6. **Metrics:** `/actuator/prometheus`. Playback counters are `vyroflix.playback.authorization` (outcome, reason) and `vyroflix.playback.master_key.lookup` (result).

---

## 6. Keep the backend awake (optional)

Render Free sleeps after about 15 minutes without traffic. [`.github/workflows/keep-alive.yml`](../.github/workflows/keep-alive.yml) pings `/actuator/health` every 10 minutes once you set a repository variable: **Settings → Secrets and variables → Actions → Variables → `BACKEND_URL`** = `https://<service>.onrender.com`. GitHub's scheduler can run late under load. The frontend also shows a "server waking up" message during cold starts.

---

## 7. Vercel (frontend)

1. Import the repository. Set **Root Directory** to `apps/web`.
2. Environment variables (all required):

   | Variable | Value |
   |---|---|
   | `NEXT_PUBLIC_GATEWAY_URL` | Your Render URL, no trailing slash. `next.config.mjs` rewrites `/api/v1/**` to it. |
   | `NEXT_PUBLIC_SUPABASE_URL` | Supabase project URL |
   | `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Supabase `anon` key |

   The app throws at sign-in if the Supabase variables are missing. It has no built-in fallback values.
3. Deploy. Add the Vercel domain to the Supabase URL configuration and to `CORS_ALLOWED_ORIGINS` on Render.

---

## 8. Encoding worker (runs outside the cloud)

FFmpeg needs CPU and disk, so the encoder runs on a machine you control. It consumes `video.uploaded.v1` and publishes `video.encoded.v1`.

1. Install FFmpeg and make it available on `PATH`.
2. Put the same Supabase, Upstash, and R2 values as Render in `.env`. Do not commit `.env`.
3. Run `scripts/run-encoding-worker.ps1` (Windows) from the repository root. It starts the `encoding-service` with the `worker` profile on port 8084.
4. If FFmpeg is missing, the script warns that it will generate synthetic output. Do not treat synthetic output as real media.

While the worker is offline, the admin page shows "encoding service not reachable" instead of fake jobs.

---

## 9. Verify the live deployment

Run these checks after each deployment. They are the evidence this guide cannot give you by itself.

1. **Outbox is publishing (Supabase SQL Editor).** Pending rows should drain to zero within a minute of activity:
   ```sql
   select 'content' as schema, status, count(*) from content.outbox_events group by status
   union all select 'streaming', status, count(*) from streaming.outbox_events group by status
   union all select 'history', status, count(*) from history.outbox_events group by status
   union all select 'identity', status, count(*) from identity.outbox_events group by status;
   ```
   If rows stay `PENDING`, check the relay logs on Render for `OutboxRelayScheduler` errors. If a schema has no rows at all while events are expected, the outbox table resolution is wrong for that service.
2. **Signed playback works.** Sign in, play a published title, and confirm the manifest URL contains `exp=` and `sig=`. Then confirm the same path with the `sig` altered returns 403.
3. **Concurrency limit.** On a `FREE` account, start playback in one tab and try a second title in another tab. The second request should return 429 (`CONCURRENT_STREAM_LIMIT`). Closing the first tab should free the slot.
4. **Roles.** A `USER` token gets 403 on `/api/v1/admin/**`, `/api/v1/videos/**` and `/api/v1/encoding/**`. Forging `X-User-Id` changes nothing.
5. **Idempotency.** Publish a title, then re-run an encoding event (or replay the message) and confirm the search index and the trending counts are not doubled.
6. **Logs.** Search the Render logs for `Bearer`, `sig=` and `X-Amz`. There should be no matches.

---

## 10. Cost and quota guardrails

- **Render Free** sleeps after about 15 minutes idle. The first request after sleep takes 30–60 seconds. Keep-alive (§6) reduces this.
- **Supabase Free** pauses after a week of inactivity and allows a limited number of direct connections. The pool is capped at 5 (`HIKARI_MAX_POOL_SIZE`).
- **Upstash Kafka Free** has a daily message quota. Each playback creates one `playback.started` and one `playback.ended` event.
- **R2** charges for stored bytes and operations. Playlist and segment requests reach R2 through the Worker, so watch operation counts in the R2 dashboard.
- **Vercel Hobby** is for personal, non-commercial use.
- A free deployment is a demo. It has no load-test evidence and no availability guarantee.
