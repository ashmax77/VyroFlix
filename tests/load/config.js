// Shared configuration for the VyroFlix k6 load tests.
//
//   BASE_URL   API Gateway (or platform) base URL      default http://localhost:8080
//   TOKEN      Supabase JWT sent as a Bearer token      default none (use mock auth locally)
//   TITLE_ID   Published seeded title                   default Cosmic Odyssey (V2__seed_catalog.sql)
//
// Local runs with VYROFLIX_SECURITY_MOCK_AUTH_ENABLED=true do not need TOKEN.

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const TITLE_ID = __ENV.TITLE_ID || 'b0000000-0000-0000-0000-000000000001';

export function headers(extra = {}) {
  const h = { 'Content-Type': 'application/json', ...extra };
  if (__ENV.TOKEN) {
    h.Authorization = `Bearer ${__ENV.TOKEN}`;
  }
  return h;
}

// Ramp to a modest number of virtual users; free-tier hosting is a demo
// environment, so do not read these numbers as capacity claims.
export const defaultStages = [
  { duration: '30s', target: 10 },
  { duration: '1m', target: 10 },
  { duration: '15s', target: 0 },
];

export const defaultThresholds = {
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<800'],
};
