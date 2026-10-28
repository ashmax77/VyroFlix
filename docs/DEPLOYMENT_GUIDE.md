# VyroFlix — Zero-Cost Cloud Deployment Guide (Phase 4)

This guide documents the exact steps to deploy **VyroFlix** to the cloud completely within free-tier quotas (**$0.00/month**).

---

## 1. Cloud Architecture Overview

| Component | Cloud Provider | Free Tier Allowance | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend** | **Vercel Hobby** | 100 GB egress/mo, 1M invocations | Next.js 14 App Router, static assets, client hydration |
| **Backend** | **Render Free** | 750 free instance hours/mo, 512 MB RAM | Combined Spring Boot platform (REST APIs, Auth, Catalog) |
| **Database & Auth** | **Supabase Free** | 500 MB PostgreSQL, 50,000 MAU Auth | Metadata schemas, users, titles, history, search index |
| **Cache & Keys** | **Upstash Redis** | 10,000 commands/day, 256 MB | HLS master playlist key cache, rate-limiting tokens |
| **Event Bus** | **Upstash Kafka** | 10,000 messages/day | Async events (`video.uploaded`, `video.encoded`) |
| **Object Store & CDN** | **Cloudflare R2** | 10 GB storage/mo, 10M reads, 1M writes | Raw video uploads, multi-bitrate HLS segments, CDN edge |

---

## 2. Step 1: Set Up Supabase (PostgreSQL & Auth)

1. Go to [supabase.com](https://supabase.com) and create a free project named `vyroflix`.
2. Choose a strong database password (store it safely).
3. Select your closest geographical region.
4. Once the project is active:
   - Go to the **SQL Editor** in the left sidebar.
   - Open [`infra/supabase/01-init-supabase-schemas.sql`](../infra/supabase/01-init-supabase-schemas.sql).
   - Copy the entire SQL contents, paste into the Supabase SQL editor, and click **Run**.
   - This creates all 8 microservice schemas: `identity`, `content`, `video`, `encoding`, `streaming`, `history`, `search`, `recommendation`.
5. Under **Project Settings -> Database**:
   - Copy the **Connection String** from the **Connect** dialog. Prefer the **Session pooler** string (IPv4-compatible); the direct `db.<PROJECT_REF>.supabase.co` host is IPv6-only and may not be reachable from Render.
   - Example JDBC URL (the username is `postgres.<PROJECT_REF>` when using the pooler):
     ```text
     jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require
     ```
6. **Admin accounts:** roles are read only from the JWT's `app_metadata.role` (users can edit `user_metadata`, so it is never trusted). Promote an administrator in the SQL editor, then have them sign in again:
   ```sql
   update auth.users
   set raw_app_meta_data = coalesce(raw_app_meta_data, '{}'::jsonb) || '{"role": "ADMIN"}'::jsonb
   where email = 'admin@example.com';
   ```
7. Under **Project Settings -> API**:
   - Copy `Project URL` (e.g., `https://xyz.supabase.co`).
   - Copy `anon` public key (used for frontend Auth).

---

## 3. Step 2: Set Up Upstash (Serverless Redis & Kafka)

1. Sign up at [upstash.com](https://upstash.com).
2. **Create Redis Database**:
   - Name: `vyroflix-redis`.
   - Eviction: enabled (`volatile-lru`).
   - Copy the `UPSTASH_REDIS_URL` or Host / Password / Port (with TLS enabled).
3. **Create Kafka Cluster**:
   - Name: `vyroflix-kafka`.
   - Cluster Type: Free Serverless.
   - Create Topics (full list and payloads: [`events/EVENT_CATALOG.md`](events/EVENT_CATALOG.md)):
     * `video.uploaded.v1`
     * `video.encoded.v1`
     * `catalog.title.published.v1`
     * `catalog.title.unpublished.v1`
   - Copy the Bootstrap Server endpoint and SASL credentials (SCRAM-SHA-256).

---

## 4. Step 3: Set Up Cloudflare R2 (Media Data Plane)

1. Sign in to your [Cloudflare Dashboard](https://dash.cloudflare.com) and navigate to **R2**.
2. Create two buckets:
   * `vyroflix-raw` (Private: raw uploads from admins).
   * `vyroflix-content` (Public CDN domain or presigned delivery: HLS segments and playlists).
3. Under **Manage R2 API Tokens**, click **Create API Token**:
   * Permissions: `Object Read & Write`.
   * Apply to specific buckets: `vyroflix-raw` and `vyroflix-content`.
   * Copy the **Access Key ID**, **Secret Access Key**, and **Endpoint URL** (e.g. `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`).

---

## 5. Step 4: Deploy Backend to Render Free

1. Log in to [render.com](https://render.com).
2. Click **New +** -> **Blueprint**.
3. Connect your GitHub repository: `ashmax77/VyroFlix`.
4. Render automatically detects [`render.yaml`](../render.yaml).
5. In the Render prompt, fill in your environment secrets from Steps 1–3:
   * `SPRING_DATASOURCE_URL`: your Supabase JDBC URL (`jdbc:postgresql://...`).
   * `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`: your Supabase database credentials.
   * `SUPABASE_URL`: your Supabase project URL (used to derive the JWKS URI and issuer for JWT validation).
   * `CORS_ALLOWED_ORIGINS`: comma-separated browser origins allowed to call the API, e.g. `https://vyroflix.vercel.app` (defaults to `http://localhost:3000` and `https://*.vercel.app`; set your exact domain, and add any custom domain).
   * `UPSTASH_REDIS_URL`: your Upstash Redis connection string (`rediss://...`).
   * `KAFKA_BOOTSTRAP_SERVERS` and `KAFKA_SASL_JAAS_CONFIG`: your Kafka endpoint and SASL credentials.
   * `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`: your Cloudflare R2 endpoint and API token.
   * See [`.env.example`](../.env.example) section 3 for the complete list.
6. Click **Apply**.
7. Render will build the multi-stage Docker container and start `vyroflix-backend`.
8. Once deployed, note your public Render URL (e.g. `https://vyroflix-backend.onrender.com`).
9. Verify by navigating to `https://vyroflix-backend.onrender.com/actuator/health` in your browser.

---

## 6. Step 5: Deploy Frontend to Vercel Hobby

1. Log in to [vercel.com](https://vercel.com).
2. Click **Add New...** -> **Project**.
3. Select the `VyroFlix` repository.
4. Set **Root Directory** to `apps/web`.
5. Under **Environment Variables**, add:
   * `NEXT_PUBLIC_GATEWAY_URL`: your Render backend URL (e.g. `https://vyroflix-backend.onrender.com`). `next.config.mjs` proxies `/api/v1/**` to it.
   * `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`: your Supabase project URL and public anon key (required: the app throws at startup if they are missing; there are no built-in fallback values).
6. Click **Deploy**.
7. Vercel builds the Next.js App Router and provisions your live URL (e.g. `https://vyroflix.vercel.app`).

---

## 7. Cost & Quota Guardrails

To ensure this deployment remains permanently at **$0.00**:

1. **Render Free Cold Starts**:
   - Render free web services spin down after 15 minutes of inactivity.
   - The first request after a sleep period takes ~30–50 seconds to boot. The frontend retries idempotent GETs with backoff (about 35 s in total) on network errors and 502/503/504, and shows a "server may be waking up" hint after 6 s of loading. Non-idempotent requests are not retried; the user sees an error with a Retry button.
2. **Supabase Connections**:
   - Supabase Free limits direct connections to 15.
   - Our `application-render.yml` sets `hikari.maximum-pool-size: 5` to safely stay within limits.
3. **Bandwidth / Video Bytes**:
   - Because video bytes stream directly between Cloudflare R2/CDN and the viewer's browser, **zero video bytes flow through Render or Vercel**, completely protecting you from server bandwidth overage fees.
