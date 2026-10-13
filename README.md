# VyroFlix

[![CI](https://github.com/ashmax77/VyroFlix/actions/workflows/ci.yml/badge.svg)](https://github.com/ashmax77/VyroFlix/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-Ready-blue.svg)

VyroFlix is an educational, cloud-native video-on-demand platform designed to demonstrate how a modern streaming service can be built with Spring Boot, domain-oriented microservices, Docker, Kafka, object storage, FFmpeg, HLS, and a Next.js frontend.

The platform is designed for two primary users:

- **Viewers** browse a catalog, search for titles, watch content, save progress, and manage a watchlist.
- **Administrators** manage catalog metadata, upload source videos, monitor encoding, and publish ready content.

The repository is currently in its **foundation phase**. Shared security, storage, messaging, outbox, database, gateway, and local infrastructure foundations are in place. The frontend and most domain workflows are planned next.

## Contents

- [Product Overview](#product-overview)
- [Current Status](#current-status)
- [Architecture](#architecture)
- [Media Workflow](#media-workflow)
- [Services](#services)
- [Technology](#technology)
- [Local Development](#local-development)
- [Documentation](#documentation)
- [License](#license)

## Product Overview

VyroFlix separates application control from media delivery.

The Spring Boot backend handles authentication, authorization, catalog metadata, upload coordination, playback authorization, history, search, recommendations, and event processing. Video files are handled by S3-compatible object storage and a CDN so large media files do not pass through the API Gateway or application services.

The intended product capabilities include:

- Supabase Auth integration with JWT-based access control
- Movie and series catalog management
- Search across titles and metadata
- Direct source-video uploads using pre-signed URLs
- FFmpeg transcoding into multi-bitrate HLS
- Secure playback using short-lived signed URLs
- Watch progress, Continue Watching, and watchlists
- Deterministic recommendations and trending rails
- Local development through Docker Compose
- Health checks, metrics, structured logging, and correlation IDs

## Current Status

### Implemented foundation

- Maven multi-module Spring Boot backend
- Spring Cloud Gateway routes and security configuration
- Supabase-compatible JWT validation and USER/ADMIN role mapping
- CORS and correlation ID filters
- S3-compatible object storage abstraction for MinIO, Cloudflare R2, and Amazon S3
- Pre-signed upload and download URL generation
- Local Kafka and Upstash Kafka event-publishing abstractions
- Standard versioned event envelopes
- Transactional outbox persistence, relay, retries, and cleanup
- Initial Flyway schemas for each domain
- Docker Compose infrastructure for PostgreSQL, Redis, Kafka, MinIO, Prometheus, and Grafana
- Unit tests for shared security, storage, messaging, and outbox components

### In progress

The domain controllers, application services, Kafka consumers, FFmpeg worker, HLS player, and Next.js frontend are not yet implemented as complete user-facing workflows. The current codebase should therefore be viewed as a production-minded starting point rather than a finished streaming product.

Run the backend verification with:

```bash
mvn clean test -f services/pom.xml
```

## Architecture

VyroFlix uses domain-oriented services. Each domain owns its data and communicates through clearly defined APIs or events.

```text
Viewer or administrator
          |
          v
Next.js web application
          |
          | HTTPS / REST / JSON with JWT
          v
Spring Cloud Gateway
  JWT validation, CORS, routing,
  correlation IDs, access control
          |
          +------------------- REST --------------------+
          |                                             |
          v                                             v
Identity   Content   Video   Streaming   History   Search   Recommendation
 Service   Service  Service  Service     Service  Service     Service
          |                                             |
          +--------------- PostgreSQL -----------------+
          |
          +---------------- Kafka events ---------------+
          |       video.uploaded.v1
          |       video.encoded.v1
          |       catalog.title.published.v1
          |
          +---------------- Redis cache ----------------+
          |       playlist keys, entitlements, rate limits

Admin browser
     |
     | pre-signed upload URL
     v
S3-compatible object storage
  MinIO locally, R2/S3 in deployment
     |
     v
Encoding-Service + FFmpeg
     |
     | HLS master playlist, variant playlists, segments
     v
Object storage -> CDN -> viewer browser

Important: video bytes do not pass through Spring Boot, the API Gateway,
Render, or Vercel.
```

### Communication model

- **REST/JSON** is used when a caller needs an immediate response, such as catalog reads, upload-intent creation, playback authorization, or progress updates.
- **Kafka** is used for asynchronous state changes and long-running workflows, such as upload completion, encoding completion, catalog publication, viewing activity, search indexing, and recommendations.
- **PostgreSQL** stores durable metadata owned by each domain.
- **Redis** stores frequently accessed or short-lived data such as playlist keys, entitlement data, sessions, and rate-limit counters.
- **Object storage and the CDN** deliver media directly to the browser.

## Media Workflow

The intended upload-to-playback flow is:

1. An administrator creates or selects a draft title.
2. Video-Service creates an upload intent and returns a short-lived pre-signed URL.
3. The browser uploads the raw video directly to MinIO, Cloudflare R2, or S3.
4. Video-Service verifies the upload and publishes `video.uploaded.v1` through the outbox and Kafka.
5. Encoding-Service consumes the event and runs FFmpeg.
6. FFmpeg creates multiple HLS renditions, such as 1080p, 720p, and 480p.
7. Encoding-Service uploads the master playlist, variant playlists, and segments to object storage.
8. Encoding-Service publishes `video.encoded.v1` after verifying the output.
9. Streaming-Service stores the master-playlist key in Redis and authorizes playback requests.
10. The viewer requests a playback session with a valid JWT.
11. Streaming-Service returns a short-lived signed URL for the HLS manifest.
12. The browser player downloads HLS playlists and segments directly from object storage or the CDN.

HLS allows a player to switch between video qualities as network conditions change. FFmpeg performs the conversion from one raw source file into the HLS playlists and segments required by the browser player.

## Services

| Service | Responsibility |
|---|---|
| **API Gateway** | Public backend entry point, routing, JWT validation, CORS, correlation IDs, and access control. |
| **Identity Service** | Internal profiles, roles, and device-session metadata integrated with Supabase Auth. |
| **Content-Service** | Movies, series, seasons, episodes, genres, cast, artwork, collections, and publication state. |
| **Video-Service** | Upload intents, pre-signed upload URLs, raw-upload metadata, and upload verification. |
| **Encoding-Service** | Kafka-driven FFmpeg processing, HLS packaging, output verification, and encoding status. |
| **Streaming-Service** | Playback authorization, entitlement checks, playback sessions, and signed HLS URLs. |
| **History Service** | Progress heartbeats, Continue Watching, watchlists, completion records, and viewing events. |
| **Search Service** | A denormalized search read model with PostgreSQL full-text search initially. |
| **Recommendation Service** | Deterministic trending, genre-based, and similar-title recommendations with caching. |

For low-cost deployment, the logical domain services may run in a combined Platform application while preserving their package and data ownership boundaries. Encoding remains suitable for a separate process because FFmpeg is CPU- and disk-intensive.

## Technology

### Application

| Technology | Purpose |
|---|---|
| Java 21 | Backend language |
| Spring Boot 3.3.5 | Backend application framework |
| Spring Cloud Gateway | API Gateway and edge routing |
| Spring Security OAuth2 Resource Server | JWT validation and authorization |
| Spring Data JPA | Relational persistence |
| Flyway | Database schema migrations |
| Next.js, React, TypeScript | Planned web application |
| hls.js | Planned browser HLS playback |

### Data and integration

| Technology | Purpose |
|---|---|
| PostgreSQL | Durable domain metadata |
| Redis | Caching, rate limiting, entitlement and session data |
| Apache Kafka | Local asynchronous event bus |
| Upstash Kafka | Cloud-friendly Kafka REST integration |
| MinIO | Local S3-compatible object storage |
| Cloudflare R2 / Amazon S3 | Hosted object storage |
| FFmpeg | Planned video transcoding and HLS packaging |

### Operations

| Technology | Purpose |
|---|---|
| Docker Compose | Local infrastructure orchestration |
| Prometheus | Metrics collection |
| Grafana | Metrics dashboards |
| Vercel | Planned frontend deployment |
| Render | Low-cost backend deployment option |

## Local Development

### Prerequisites

- Java 21 JDK
- Maven
- Node.js and npm for the planned frontend
- Docker Desktop with Docker Compose
- Git

### Start local infrastructure

```bash
docker compose up -d
```

The Compose environment provides:

- PostgreSQL on port `5432`
- Redis on port `6379`
- Kafka on port `9092`
- Kafka UI at `http://localhost:8090`
- MinIO API on port `9000`
- MinIO Console at `http://localhost:9001`
- Prometheus at `http://localhost:9090`
- Grafana at `http://localhost:3001`

MinIO initializes local buckets for raw media and HLS content. These buckets provide an S3-compatible local alternative to Cloudflare R2.

### Build and test the backend

```bash
mvn clean test -f services/pom.xml
mvn clean package -DskipTests -f services/pom.xml
```

### Run the backend locally

Run the combined platform application:

```bash
mvn spring-boot:run -pl platform -f services/pom.xml -Dspring-boot.run.profiles=local
```

Run the API Gateway in a separate terminal:

```bash
mvn spring-boot:run -pl api-gateway -f services/pom.xml -Dspring-boot.run.profiles=local
```

The Gateway health endpoint is available at `http://localhost:8080/actuator/health`.

## Documentation

| Document | Description |
|---|---|
| [PROJECT_REQUIREMENTS.md](PROJECT_REQUIREMENTS.md) | Product scope, functional requirements, quality requirements, and acceptance criteria. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Service boundaries, data ownership, API contracts, events, media flow, and deployment strategy. |
| [AGENTS.md](AGENTS.md) | Repository implementation rules and engineering constraints. |

## License

This project is for educational and portfolio purposes. Use open-licensed sample videos, trailers, or generated test media only. Do not host copyrighted or unlicensed content.
