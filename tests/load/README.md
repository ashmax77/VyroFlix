# Load tests (k6)

k6 scripts for the hot read/write paths named in `docs/PROJECT_REQUIREMENTS.md`:
browse, search, playback authorization, and progress updates.

| Script | Endpoints exercised |
|---|---|
| `browse.js` | `GET /api/v1/catalog/home`, `/catalog/titles`, `/catalog/titles/{id}`, `/recommendations/trending` |
| `search.js` | `GET /api/v1/search`, `/search/suggestions` |
| `playback.js` | `POST /api/v1/streaming/sessions` (authorization only — no video bytes) |
| `progress.js` | `PUT /api/v1/history/progress` |

## Run

Install [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/), start the stack
(see the root `README.md`), then:

```bash
k6 run tests/load/browse.js
k6 run -e BASE_URL=http://localhost:8080 tests/load/search.js
k6 run -e TOKEN=<supabase-jwt> -e BASE_URL=https://<backend> tests/load/playback.js
```

Variables: `BASE_URL` (default `http://localhost:8080`), `TOKEN` (Bearer JWT; omit when
`VYROFLIX_SECURITY_MOCK_AUTH_ENABLED=true` locally), `TITLE_ID` (default: the seeded
*Cosmic Odyssey* from `V2__seed_catalog.sql`).

Each script ramps to 10 virtual users and asserts `http_req_failed < 1%` and
`p95 < 800 ms`. To keep results out of git, write them under `tests/load/results/` (gitignored):

```bash
k6 run --summary-export tests/load/results/browse.json tests/load/browse.js
```

## Notes

- Do **not** point these at the Render Free deployment expecting meaningful capacity numbers:
  the instance has 512 MB RAM, sleeps when idle, and the first request pays a cold start.
  Treat results as a smoke/regression signal, not a claim about concurrent streams.
- The API gateway rate-limits per client (default 200 requests/minute); a single-machine
  test will hit `429` if pushed past it. Raise `VYROFLIX_GATEWAY_RATE_LIMIT_REQUESTS_PER_MINUTE`
  for local runs.
- These scripts have not been executed in this repository yet.
