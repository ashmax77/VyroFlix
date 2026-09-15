# 🎬 VyroFlix

[![CI](https://github.com/ashmax77/VyroFlix/actions/workflows/ci.yml/badge.svg)](https://github.com/ashmax77/VyroFlix/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-Ready-blue.svg)

A cloud-native, video-on-demand streaming platform built with a Spring Boot microservice backend and a Next.js/React frontend. Inspired by modern streaming products, VyroFlix demonstrates production-grade architecture with asynchronous event-driven workflows, HLS video delivery, and a polished consumer web experience.

---

## 📖 Table of Contents

- [Overview](#overview)
- [Core Features](#core-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Documentation](#documentation)
- [License](#license)

---

## Overview

VyroFlix is a full-stack video streaming platform that allows **viewers** to browse, search, and stream content with resume-playback support, and **administrators** to manage the content lifecycle from upload through transcoding to publication. All media is delivered via HLS through object storage/CDN — video bytes never travel through the application services.

### Product Goals

- Production-minded Spring Boot microservice architecture.
- Asynchronous, event-driven workflows powered by Kafka.
- Video delivery through object storage and CDN, not application servers.
- Polished, responsive consumer web application.
- Full local development environment via Docker Compose.
- Incrementally deployable to Kubernetes or AWS.
- Built-in testability, security, documentation, and observability.

---

## Core Features

### 🎥 For Viewers

| Feature | Description |
|---|---|
| **Authentication** | Secure registration and login with JWT access tokens and refresh-token rotation. |
| **Catalog Browsing** | Browse movies, series, genres, collections, and curated rails (Trending, Recently Added, etc.). |
| **Search** | Full-text search across titles, genres, cast, and tags with filters and pagination. |
| **HLS Video Playback** | Stream content via an in-browser HLS player with play/pause, seek, volume, fullscreen, and quality selection. |
| **Watch Progress** | Automatic progress persistence with heartbeats every 15–30 seconds; resume playback across sessions. |
| **Continue Watching** | Unfinished content surfaced in a dedicated rail, ordered by most recently watched. |
| **Watchlist** | Personal watchlist to save titles for later viewing. |
| **Recommendations** | Deterministic content rails including Trending, genre-based, and Similar Titles. |
| **Subscriptions** | Plan-based entitlement (FREE, BASIC, PREMIUM) with mock billing in Phase 1. |

### 🛠️ For Administrators

| Feature | Description |
|---|---|
| **Content Management** | Create, edit, publish, and unpublish movies and series with full metadata. |
| **Media Ingestion** | Upload source video via pre-signed URLs; automatic transcoding to HLS multi-bitrate variants. |
| **Transcode Monitoring** | Track processing state (QUEUED → PROCESSING → READY / FAILED) with retry support. |
| **Publication Control** | Titles can only be published when all required media assets are in READY state. |
| **Analytics** | Basic platform analytics and observability dashboards. |

### 📡 Platform & Operations

- Health, readiness, and liveness probes on every service.
- Prometheus metrics endpoint.
- Structured JSON logging with correlation IDs.
- Distributed trace propagation across gateway, services, and async events.
- Dead-letter topic handling for failed event processing.

---

## Architecture

VyroFlix follows a **domain-oriented microservices** architecture. Each service owns its database schema and data. Services communicate synchronously via REST/JSON when an immediate response is required, and asynchronously through Kafka for state-change events using the outbox pattern.

```
                         ┌──────────────────────┐
                         │  Next.js Web Client   │
                         │  React + TypeScript   │
                         └──────────┬────────────┘
                                    │
                           HTTPS / REST / JSON
                                    │
                         ┌──────────▼────────────┐
                         │ Spring Cloud Gateway   │
                         │ Auth · CORS · Routing  │
                         │ Rate Limiting          │
                         └──────────┬────────────┘
                                    │
      ┌─────────────┬───────────────┼───────────────┬──────────────┐
      │             │               │               │              │
 ┌────▼────┐  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐  ┌────▼──────┐
 │Identity │  │ Catalog   │  │ Playback  │  │ History   │  │ Search    │
 │Service  │  │ Service   │  │ Service   │  │ Service   │  │ Service   │
 └────┬────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └────┬──────┘
      │             │               │               │              │
   Postgres      Postgres       Postgres         Postgres     Postgres/
      │             │               │               │          OpenSearch
      └─────────────┴───────────────┴───────────────┴──────────────┘
                                    │
                             ┌──────▼──────┐
                             │    Kafka     │
                             │  Event Bus   │
                             └──────┬──────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
             ┌──────▼──────┐               ┌───────▼──────────┐
             │   Media     │               │ Recommendation   │
             │   Service   │               │ Service          │
             └──────┬──────┘               └──────────────────┘
                    │
         ┌──────────▼───────────┐
         │   Object Storage     │
         │   MinIO / Amazon S3  │
         └──────────┬───────────┘
                    │
         ┌──────────▼───────────┐
         │   CDN / CloudFront   │
         └──────────────────────┘
```

### Media Delivery Model

1. Browser requests playback authorization from the Playback Service.
2. Playback Service returns a short-lived signed URL to the HLS master manifest.
3. Browser player fetches the master playlist and segments directly from CDN/object storage.
4. CDN handles repeated segment delivery and geographic scaling.
5. Browser sends progress heartbeats to the History Service via the API Gateway.
6. **Video bytes never travel through the API Gateway or Spring services.**

---

## Tech Stack

### Backend

| Technology | Purpose |
|---|---|
| **Java 21** | Primary language |
| **Spring Boot 3.x** | Service framework |
| **Spring Cloud Gateway** | API gateway, edge routing, CORS, rate limiting |
| **Spring Security** | Authentication and authorization (JWT) |
| **Spring Data JPA** | Database access and ORM |
| **PostgreSQL** | Primary relational database (one per service) |
| **Redis** | Caching, session state, rate limiting |
| **Apache Kafka** | Asynchronous event bus with outbox pattern |
| **Flyway** | Database schema migrations |
| **MinIO / Amazon S3** | Object storage for media assets |
| **FFmpeg** | Local video transcoding to HLS |
| **Testcontainers** | Integration testing with real infrastructure |
| **OpenAPI / Swagger** | API documentation |
| **Docker** | Containerization |

### Frontend

| Technology | Purpose |
|---|---|
| **Next.js** | React framework with SSR/SSG |
| **React** | UI component library |
| **TypeScript** | Type-safe JavaScript |

### Infrastructure & Observability

| Technology | Purpose |
|---|---|
| **Docker Compose** | Local development orchestration |
| **Prometheus** | Metrics collection |
| **Structured JSON Logging** | Centralized, searchable logs |
| **Correlation ID Propagation** | Distributed request tracing |
| **Vercel** | Frontend deployment (Hobby plan) |
| **Render** | Backend deployment (Free plan) |
| **Cloudflare R2 + CDN** | Object storage and media delivery |

---

## Services

| Service | Responsibility | Database |
|---|---|---|
| **API Gateway** | Edge routing, CORS, JWT validation, rate limiting, correlation ID propagation | — |
| **Identity Service** | Supabase Auth integration, internal user profiles, roles (USER/ADMIN) | PostgreSQL |
| **Content-Service** | Movies, series, seasons, episodes, genres, cast, publication state, home-page rails | PostgreSQL |
| **Video-Service** | Pre-signed upload URLs, upload intent tracking, raw video ingestion | PostgreSQL + Object Storage |
| **Encoding-Service** | FFmpeg transcoding, multi-bitrate HLS packaging, encoding job management | PostgreSQL + Object Storage |
| **Streaming-Service** | Playback session creation, entitlement checks, signed HLS URL generation | PostgreSQL + Redis |
| **History Service** | Watch progress, continue watching, watchlist, completion tracking | PostgreSQL |
| **Search Service** | Denormalized search index, full-text search, filters, lightweight search cards | PostgreSQL (Phase 1) → OpenSearch (Phase 2) |
| **Recommendation Service** | Trending, genre-based, and similar-title rails; cached responses; graceful degradation | Redis + PostgreSQL |

---

## Project Structure

```
VyroFlix/
├── apps/
│   └── web/                          # Next.js App Router frontend
├── services/                         # Spring Boot microservices (Maven multi-module)
│   ├── pom.xml                       # Parent POM — dependency management
│   ├── common/                       # Shared library module
│   ├── platform/                     # Combined deployable module for Render
│   ├── api-gateway/
│   ├── identity-service/
│   ├── content-service/
│   ├── video-service/
│   ├── encoding-service/
│   ├── streaming-service/
│   ├── history-service/
│   ├── search-service/
│   └── recommendation-service/
├── infra/
│   ├── docker/                       # Docker-related configs & init scripts
│   ├── observability/                # Prometheus, Grafana configs
│   ├── render/                       # Render deployment config
│   └── vercel/                       # Vercel deployment config
├── docs/
│   ├── api/                          # OpenAPI specs
│   └── events/                       # Event schema documentation
├── tests/
│   └── load/                         # k6 / Gatling load tests
├── .env.example
├── docker-compose.yml
├── AGENTS.md                         # Agent implementation rules
├── ARCHITECTURE.md                   # System architecture and design
├── PROJECT_REQUIREMENTS.md           # Functional and non-functional requirements
└── README.md                         # This file
```

---

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Node.js 18+** and **npm**
- **Docker** and **Docker Compose**
- **Git**

### Local Development

1. **Clone the repository and set up environment**

   ```bash
   git clone <repository-url>
   cd VyroFlix
   cp .env.example .env
   ```

2. **Start infrastructure with Docker Compose**

   ```bash
   docker compose up -d
   ```

   This launches the zero-cost local baseline:
   - **PostgreSQL 16**: Port `5432` (auto-provisions 8 service schemas)
   - **Redis 7**: Port `6379`
   - **Apache Kafka 3.7 (KRaft)**: Port `9092`
   - **Kafka UI**: Port `8090` (`http://localhost:8090`)
   - **MinIO S3**: Port `9000` (API), Port `9001` (`http://localhost:9001` Web Console)
   - **Prometheus**: Port `9090` (`http://localhost:9090`)
   - **Grafana**: Port `3001` (`http://localhost:3001`, user/pass: `admin`/`admin`)

3. **Build & Test backend services**

   ```bash
   # Run unit and integration tests across all 12 modules
   mvn clean test -f services/pom.xml

   # Build executable JAR packages
   mvn clean package -DskipTests -f services/pom.xml
   ```

4. **Run backend applications (Local Profile)**

   You can run services individually or launch the combined deployment:

   ```bash
   # Option A: Combined platform (all 7 domain services in 1 process)
   mvn spring-boot:run -pl platform -f services/pom.xml -Dspring-boot.run.profiles=local

   # Start API Gateway (handles edge routing, JWT auth, and rate limiting)
   mvn spring-boot:run -pl api-gateway -f services/pom.xml -Dspring-boot.run.profiles=local

   # Or run any standalone microservice (e.g., content-service)
   mvn spring-boot:run -pl content-service -f services/pom.xml -Dspring-boot.run.profiles=local
   ```

5. **Run the web frontend**

   ```bash
   cd apps/web
   npm install
   npm run dev
   ```

6. **Access the application**

   - **Web Application**: `http://localhost:3000`
   - **API Gateway**: `http://localhost:8080` (health: `http://localhost:8080/actuator/health`)
   - **MinIO Console**: `http://localhost:9001` (`vyroflix` / `vyroflix123`)
   - **Kafka UI**: `http://localhost:8090`
   - **Grafana Dashboards**: `http://localhost:3001`


---

## Documentation

| Document | Description |
|---|---|
| [PROJECT_REQUIREMENTS.md](PROJECT_REQUIREMENTS.md) | Product scope, functional requirements, quality requirements, flows, and Phase 1 acceptance criteria |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Microservices, service ownership, event contracts, API contracts, media-delivery flow, and failure behaviour |
| [AGENTS.md](AGENTS.md) | Repository-wide implementation rules, coding standards, and delivery order |

---

## License

This project is for educational and portfolio purposes. Use open-licensed sample videos, trailers, or generated test media only. Do not host copyrighted or unlicensed content.
