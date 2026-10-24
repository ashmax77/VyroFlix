import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TITLE_ID, headers, defaultStages, defaultThresholds } from './config.js';

export const options = { stages: defaultStages, thresholds: defaultThresholds };

const DURATION_SECONDS = 7200;

// Progress updates: mimics the player's 15–30s heartbeat. The endpoint is an
// idempotent upsert, so repeating positions for the same title is expected.
export default function () {
  let position = Math.floor(Math.random() * 600);

  for (let beat = 0; beat < 4; beat++) {
    const body = JSON.stringify({
      contentId: TITLE_ID,
      targetType: 'MOVIE',
      positionSeconds: position,
      durationSeconds: DURATION_SECONDS,
    });

    const res = http.put(`${BASE_URL}/api/v1/history/progress`, body, { headers: headers() });
    check(res, { 'progress accepted (2xx)': (r) => r.status >= 200 && r.status < 300 });

    position += 20;
    sleep(2); // compressed heartbeat interval for the test
  }
}
