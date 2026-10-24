# VyroFlix Event Catalog

All cross-service state changes travel over Kafka using a **versioned event type** and a common
envelope. Producers write an outbox row in the same transaction as the state change; the outbox
relay publishes it. Consumers are idempotent and deduplicate on `eventId`; poison messages go to
a `<topic>.DLT` dead-letter topic after 3 retries with exponential backoff (1s, 2s, 4s).

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

Events never contain credentials, JWTs, signed URLs, passwords, or unnecessary PII.
Topic names are defined in `com.vyroflix.common.util.Constants`.

## Active events (produced and consumed)

| Event / topic | Producer | Consumers | Meaning |
|---|---|---|---|
| `video.uploaded.v1` | Video-Service | Encoding-Service | A raw upload was verified in object storage; start transcoding. |
| `video.encoded.v1` | Encoding-Service | Streaming-Service, Content-Service | HLS output verified. Streaming caches the master-playlist key in Redis; Content marks the media asset `READY`. |
| `catalog.title.published.v1` | Content-Service | Search-Service, Recommendation-Service | Title became `PUBLISHED`; update read models. |
| `catalog.title.unpublished.v1` | Content-Service | Search-Service | Title became `UNPUBLISHED`; mark it so in the index. |
| `playback.started.v1` | Streaming-Service | Recommendation-Service | A playback session was created. |
| `history.content.completed.v1` | History-Service | Recommendation-Service | A viewer crossed the completion threshold. |

## Produced, no consumer yet

| Event / topic | Producer | Notes |
|---|---|---|
| `user.registered.v1` | Identity-Service | Emitted on first profile sync. |
| `watchlist.item.added.v1` | History-Service | Watchlist add. |
| `watchlist.item.removed.v1` | History-Service | Watchlist remove. |

## Declared but not yet emitted

`catalog.title.created.v1`, `catalog.title.updated.v1`, `history.progress.updated.v1`,
`playback.ended.v1` exist as constants but have no producer or consumer in the code today.

## Upload-to-playback flow

```text
Video-Service --video.uploaded.v1--> Encoding-Service
Encoding-Service --video.encoded.v1--> Streaming-Service (Redis key) + Content-Service (asset READY)
Content-Service --catalog.title.published.v1--> Search-Service + Recommendation-Service
```

## Configuration

- Local: Apache Kafka (`MESSAGING_TYPE=kafka`, `KAFKA_BOOTSTRAP_SERVERS`).
- Cloud: any SASL_SSL Kafka (`KAFKA_SECURITY_PROTOCOL`, `KAFKA_SASL_*`) or the Upstash REST producer
  (`MESSAGING_TYPE=upstash`). See [`.env.example`](../../.env.example).
- Topics are auto-created locally; create them manually on managed clusters
  (see [`../DEPLOYMENT_GUIDE.md`](../DEPLOYMENT_GUIDE.md)).
