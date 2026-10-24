import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TITLE_ID, headers, defaultStages, defaultThresholds } from './config.js';

export const options = { stages: defaultStages, thresholds: defaultThresholds };

// Browse: home feed, catalog listing, title details, trending rail.
export default function () {
  const params = { headers: headers() };

  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/catalog/home`, null, params],
    ['GET', `${BASE_URL}/api/v1/catalog/titles?type=MOVIE`, null, params],
    ['GET', `${BASE_URL}/api/v1/catalog/titles/${TITLE_ID}`, null, params],
    ['GET', `${BASE_URL}/api/v1/recommendations/trending?limit=10`, null, params],
  ]);

  responses.forEach((res, i) => {
    check(res, { [`browse request ${i} is 200`]: (r) => r.status === 200 });
  });

  sleep(1);
}
