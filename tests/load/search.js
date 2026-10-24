import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, headers, defaultStages, defaultThresholds } from './config.js';

export const options = { stages: defaultStages, thresholds: defaultThresholds };

const QUERIES = ['cosmic', 'shadow', 'cyber', 'detective', 'echo', 'blue'];

// Search: full-text query plus autocomplete suggestions.
export default function () {
  const q = QUERIES[Math.floor(Math.random() * QUERIES.length)];
  const params = { headers: headers() };

  const search = http.get(`${BASE_URL}/api/v1/search?q=${q}&page=0&size=20`, params);
  check(search, { 'search is 200': (r) => r.status === 200 });

  const suggest = http.get(`${BASE_URL}/api/v1/search/suggestions?q=${q.slice(0, 3)}&limit=10`, params);
  check(suggest, { 'suggestions is 200': (r) => r.status === 200 });

  sleep(1);
}
