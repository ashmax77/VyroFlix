// Run with: node --test infra/cloudflare/hls-worker/test/
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { handleRequest, rewritePlaylist, scopeOf, signToken } from '../src/index.js';

const SECRET = 'test-secret-that-is-at-least-32-characters-long';
const ASSET = 'titles/3f2b1c00-0000-4000-8000-000000000001';
const MASTER = `${ASSET}/hls/master.m3u8`;
const NOW = 1_800_000_000;

// Cross-language vector: must equal PlaybackUrlSignerTest's expected value for the same inputs.
test('signature matches the Java PlaybackUrlSigner vector', async () => {
  const sig = await signToken(SECRET, 'titles/3f2b1c00-0000-4000-8000-000000000001/', 1_900_000_000);
  assert.equal(sig, '_Wq88MfLUKJ_ycEeq8GtYbSR3mhwhI7-2l28_CbltGU');
});

function fakeEnv(objects = {}) {
  return {
    HLS_SIGNING_SECRET: SECRET,
    CHILD_TOKEN_TTL_SECONDS: '3600',
    CONTENT: {
      async get(key) {
        if (!(key in objects)) return null;
        const value = objects[key];
        return {
          body: value,
          httpMetadata: { contentType: key.endsWith('.ts') ? 'video/MP2T' : undefined },
          async text() { return value; },
        };
      },
    },
  };
}

async function tokenQuery(exp, scope = scopeOf(MASTER)) {
  return `exp=${exp}&sig=${await signToken(SECRET, scope, exp)}`;
}

test('scopeOf covers the asset folder only', () => {
  assert.equal(scopeOf(MASTER), `${ASSET}/`);
  assert.equal(scopeOf('titles/../../etc/passwd.m3u8'), null);
  assert.equal(scopeOf('master.m3u8'), null);
});

test('a valid token serves the master playlist with child URIs carrying a fresh token', async () => {
  const env = fakeEnv({ [MASTER]: '#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nplaylist_720p.m3u8\n' });
  const res = await handleRequest(
    new Request(`https://media.example/${MASTER}?${await tokenQuery(NOW + 60)}`), env, NOW);

  assert.equal(res.status, 200);
  const body = await res.text();
  assert.match(body, /playlist_720p\.m3u8\?exp=\d+&sig=[A-Za-z0-9_-]+/);
  const childExp = Number(body.match(/exp=(\d+)/)[1]);
  assert.equal(childExp, NOW + 3600, 'child tokens last CHILD_TOKEN_TTL_SECONDS, not the manifest TTL');
});

test('an expired token is rejected', async () => {
  const env = fakeEnv({ [MASTER]: '#EXTM3U\n' });
  const res = await handleRequest(
    new Request(`https://media.example/${MASTER}?${await tokenQuery(NOW - 1)}`), env, NOW);
  assert.equal(res.status, 403);
});

test('a tampered signature is rejected', async () => {
  const env = fakeEnv({ [MASTER]: '#EXTM3U\n' });
  const query = await tokenQuery(NOW + 60);
  const res = await handleRequest(
    new Request(`https://media.example/${MASTER}?${query.replace(/sig=./, 'sig=X')}`), env, NOW);
  assert.equal(res.status, 403);
});

test('a token for one asset does not open another asset', async () => {
  const other = 'titles/99999999-0000-4000-8000-000000000000/hls/master.m3u8';
  const env = fakeEnv({ [other]: '#EXTM3U\n' });
  const res = await handleRequest(
    new Request(`https://media.example/${other}?${await tokenQuery(NOW + 60)}`), env, NOW);
  assert.equal(res.status, 403);
});

test('child segments are served with a valid child token and rejected without one', async () => {
  const segment = `${ASSET}/hls/segments/seg_001.ts`;
  const env = fakeEnv({ [segment]: 'bytes' });

  const ok = await handleRequest(
    new Request(`https://media.example/${segment}?${await tokenQuery(NOW + 3600)}`), env, NOW);
  assert.equal(ok.status, 200);
  assert.equal(ok.headers.get('Content-Type'), 'video/MP2T');

  const bare = await handleRequest(new Request(`https://media.example/${segment}`), env, NOW);
  assert.equal(bare.status, 403);
});

test('non-media extensions and traversal attempts are not served', async () => {
  const env = fakeEnv({ [`${ASSET}/hls/secret.json`]: '{}' });
  const json = await handleRequest(
    new Request(`https://media.example/${ASSET}/hls/secret.json?${await tokenQuery(NOW + 60)}`), env, NOW);
  assert.equal(json.status, 404);

  const traversal = await handleRequest(
    new Request(`https://media.example/titles/../secrets/master.m3u8?${await tokenQuery(NOW + 60)}`), env, NOW);
  assert.equal(traversal.status, 404);
});

test('rewritePlaylist leaves absolute URIs and tags without URIs alone', () => {
  const input = [
    '#EXTM3U',
    '#EXT-X-KEY:METHOD=AES-128,URI="keys/k1.key"',
    '#EXTINF:6.0,',
    'segments/seg_001.ts',
    'https://other.example/x.ts',
  ].join('\n');
  const out = rewritePlaylist(input, 'exp=1&sig=abc');
  assert.match(out, /URI="keys\/k1\.key\?exp=1&sig=abc"/);
  assert.match(out, /segments\/seg_001\.ts\?exp=1&sig=abc/);
  assert.match(out, /^https:\/\/other\.example\/x\.ts$/m);
});
