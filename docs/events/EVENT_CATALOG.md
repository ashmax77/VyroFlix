# VyroFlix Event Catalog

Cross-service state changes travel over Kafka as **versioned** events in a common envelope. A producer writes an outbox row in the same transaction as the state change. A relay publishes it. Consumers are idempotent and deduplicate on `eventId` where the effect is not already an upsert. Failed records are retried with exponential backoff (1 s, 2 s, 4 s, three attempts) and then routed to `<topic>.DLT`.

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

Events never contain credentials, JWTs, signed URLs, passwords, or unnecessary personal data. Topic names are defined in `com.vyroflix.common.util.Constants`.

## Event types

| Event | Producer | Consumers | Meaning |
|---|---|---|---|
| `video.uploaded.v1` | Video | Encoding | A raw upload was verified. Start transcoding. |
| `video.encoded.v1` | Encoding | Streaming, Content | HLS output verified. Streaming stores the master-playlist key. Content marks the media asset `READY`. |
| `catalog.title.published.v1` | Content | Search, Recommendation, Streaming | A title became `PUBLISHED`. Carries `castMembers` and `directors` (optional). |
| `catalog.title.updated.v1` | Content | Search | A published title's metadata or credits changed. Same payload shape as published. |
| `catalog.title.unpublished.v1` | Content | Search, Streaming | A title became `UNPUBLISHED`. Search marks it; Streaming stops authorising it. |
| `catalog.episode.published.v1` | Content | Streaming | An episode became `PUBLISHED` with its series. Streaming links it to the series. |
| `playback.started.v1` | Streaming | Recommendation | A playback session was authorised. |
| `playback.ended.v1` | Streaming | — | A session was ended by the viewer. Produced; no consumer yet. |
| `history.content.completed.v1` | History | Recommendation | A viewer crossed the completion threshold. |
| `user.registered.v1` | Identity | — | First profile sync. Produced; no consumer yet. |
| `watchlist.item.added.v1` | History | — | Watchlist add. Produced; no consumer yet. |
| `watchlist.item.removed.v1` | History | — | Watchlist remove. Produced; no consumer yet. |

Declared constants with no producer: `catalog.title.created.v1` and `history.progress.updated.v1`. Progress is intentionally not published per heartbeat, to protect the free Kafka quota. Remove the unused constants when next touching `Constants`.

## Idempotency

| Consumer | How duplicates are handled |
|---|---|
| Content — `video.encoded.v1` | Upsert of the asset to `READY`. Re-applying is a no-op. |
| Streaming — `video.encoded.v1` | Upsert of the master-playlist key (database, then Redis). |
| Streaming — catalog events | Upserts into `title_availability`. |
| Search — catalog events | Upsert by title id. |
| Recommendation — metadata | Upsert by title id. |
| Recommendation — `playback.started.v1`, `history.content.completed.v1` | Each `eventId` is claimed in `recommendation.processed_events` in the same transaction as the counter update. A redelivery is skipped. |
| Encoding — `video.uploaded.v1` | Job created once per upload intent; a second delivery does not start a second encode. |

## Failure handling

Consumers rethrow processing failures so the Kafka error handler retries and then dead-letters the record. Consumers that parse a malformed record log and drop it instead of retrying, because retrying cannot fix it.

## Upload-to-playback flow

```text
Video     --video.uploaded.v1----------> Encoding
Encoding  --video.encoded.v1-----------> Streaming (playlist key) + Content (asset READY)
Content   --catalog.title.published.v1-> Search + Recommendation + Streaming (title available)
Content   --catalog.episode.published.v1-> Streaming (episode available)
Streaming --playback.started.v1--------> Recommendation
History   --history.content.completed.v1-> Recommendation
```

## Configuration

- Local: Apache Kafka (`MESSAGING_TYPE=kafka`, `KAFKA_BOOTSTRAP_SERVERS`).
- Cloud: any SASL_SSL Kafka (`KAFKA_SECURITY_PROTOCOL`, `KAFKA_SASL_*`), or the Upstash REST producer (`MESSAGING_TYPE=upstash`). See [`.env.example`](../../.env.example).
- Managed clusters need the topics and their `.DLT` topics created in advance. See [`../DEPLOYMENT_GUIDE.md`](../DEPLOYMENT_GUIDE.md).
