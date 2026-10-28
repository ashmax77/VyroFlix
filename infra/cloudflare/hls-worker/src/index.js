/**
 * VyroFlix HLS delivery worker.
 *
 * Sits in front of a PRIVATE R2 bucket that holds encoded HLS output. Streaming-Service issues a short-lived
 * master manifest URL carrying an HMAC token (`?exp=&sig=`). This worker:
 *   1. verifies the token against the asset folder it covers (scope = first two path segments, e.g. titles/{id}/),
 *   2. serves the master playlist, rewriting every child playlist/segment URI to carry a longer-lived token
 *      for the same folder (so long movies keep playing after the manifest URL's 15-minute expiry),
 *   3. serves segments and keys directly from R2.
 *
 * Video bytes never pass through Spring Boot. The token scope must match PlaybackUrlSigner.scopeOf in Java.
 *
 * Bindings / secrets (see wrangler.toml):
 *   CONTENT                 R2 bucket binding (vyroflix-content, private)
 *   HLS_SIGNING_SECRET      shared HMAC secret, same value as PLAYBACK_SIGNING_SECRET on the backend
 *   CHILD_TOKEN_TTL_SECONDS optional, default 14400 (4 hours)
 */

const ALLOWED_EXTENSIONS = new Set(['m3u8', 'ts', 'm4s', 'mp4', 'aac', 'vtt', 'key']);
const DEFAULT_CHILD_TTL_SECONDS = 4 * 60 * 60;
const encoder = new TextEncoder();

/** Asset folder covered by a token: "titles/{id}/" for "titles/{id}/hls/master.m3u8". Null if not allowed. */
export function scopeOf(objectKey) {
  const parts = objectKey.split('/');
  if (parts.length < 3 || !parts[0] || !parts[1] || objectKey.includes('..')) {
    return null;
  }
  return `${parts[0]}/${parts[1]}/`;
}

function base64UrlEncode(bytes) {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** HMAC-SHA256 over `${scope}\n${exp}`, base64url without padding (matches Java PlaybackUrlSigner). */
export async function signToken(secret, scope, exp) {
  const key = await crypto.subtle.importKey(
    'raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(`${scope}\n${exp}`));
  return base64UrlEncode(new Uint8Array(signature));
}

function constantTimeEquals(a, b) {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i += 1) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

function isAbsoluteOrRooted(uri) {
  return /^[a-z][a-z0-9+.-]*:/i.test(uri) || uri.startsWith('/') || uri.startsWith('#');
}

/**
 * Appends `query` to every relative URI in a playlist: plain URI lines and URI="..." attributes
 * (EXT-X-KEY, EXT-X-MAP, EXT-X-MEDIA, EXT-X-I-FRAME-STREAM-INF).
 */
export function rewritePlaylist(text, query) {
  const withQuery = (uri) => {
    if (isAbsoluteOrRooted(uri)) return uri;
    return uri + (uri.includes('?') ? '&' : '?') + query;
  };
  return text
    .split('\n')
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed === '') return line;
      if (trimmed.startsWith('#')) {
        return line.replace(/URI="([^"]+)"/g, (_, uri) => `URI="${withQuery(uri)}"`);
      }
      return withQuery(trimmed);
    })
    .join('\n');
}

function plain(status, message) {
  return new Response(message, {
    status,
    headers: { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' },
  });
}

/**
 * @param {Request} request
 * @param {{ CONTENT: R2Bucket, HLS_SIGNING_SECRET: string, CHILD_TOKEN_TTL_SECONDS?: string }} env
 * @param {number} [nowSeconds] injectable clock for tests
 */
export async function handleRequest(request, env, nowSeconds = Math.floor(Date.now() / 1000)) {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    return plain(405, 'Method not allowed');
  }
  if (!env.HLS_SIGNING_SECRET) {
    return plain(500, 'Worker is not configured');
  }

  const url = new URL(request.url);
  let key;
  try {
    key = decodeURIComponent(url.pathname.replace(/^\/+/, ''));
  } catch {
    return plain(400, 'Bad path');
  }

  const extension = key.includes('.') ? key.split('.').pop().toLowerCase() : '';
  if (!ALLOWED_EXTENSIONS.has(extension) || key.includes('\\') || key.includes('\0')) {
    return plain(404, 'Not found');
  }
  const scope = scopeOf(key);
  if (!scope) {
    return plain(404, 'Not found');
  }

  const exp = Number(url.searchParams.get('exp'));
  const sig = url.searchParams.get('sig') || '';
  if (!Number.isInteger(exp) || sig === '') {
    return plain(403, 'Forbidden');
  }
  if (exp < nowSeconds) {
    return plain(403, 'Link expired');
  }
  const expected = await signToken(env.HLS_SIGNING_SECRET, scope, exp);
  if (!constantTimeEquals(sig, expected)) {
    return plain(403, 'Forbidden');
  }

  const object = await env.CONTENT.get(key);
  if (!object) {
    return plain(404, 'Not found');
  }

  if (extension === 'm3u8') {
    const ttl = Number(env.CHILD_TOKEN_TTL_SECONDS) > 0
      ? Number(env.CHILD_TOKEN_TTL_SECONDS)
      : DEFAULT_CHILD_TTL_SECONDS;
    const childExp = nowSeconds + ttl;
    const childSig = await signToken(env.HLS_SIGNING_SECRET, scope, childExp);
    const body = rewritePlaylist(await object.text(), `exp=${childExp}&sig=${childSig}`);
    return new Response(request.method === 'HEAD' ? null : body, {
      headers: {
        'Content-Type': 'application/vnd.apple.mpegurl',
        'Cache-Control': 'private, max-age=30',
      },
    });
  }

  const contentType = object.httpMetadata?.contentType || 'application/octet-stream';
  return new Response(request.method === 'HEAD' ? null : object.body, {
    headers: {
      'Content-Type': contentType,
      'Cache-Control': 'public, max-age=86400',
    },
  });
}

export default {
  fetch(request, env) {
    return handleRequest(request, env);
  },
};
