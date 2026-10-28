package com.vyroflix.streaming.service;

import com.vyroflix.common.storage.ObjectStorageClient;
import com.vyroflix.common.util.Constants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Produces the short-lived manifest URL handed to the player.
 *
 * <p>Two modes:</p>
 * <ul>
 *   <li><b>CDN token mode</b> (when {@code vyroflix.playback.cdn-base-url} is set): an HMAC token over the
 *       encoded asset's folder scope ({@code titles/{id}/}) and an expiry. The Cloudflare Worker in
 *       {@code infra/cloudflare/hls-worker} validates the token, rewrites child playlist URLs and serves
 *       segments from a private R2 bucket. Spring never touches media bytes.</li>
 *   <li><b>Presigned R2/S3 mode</b> (local MinIO/dev): a presigned GET for the master playlist only.</li>
 * </ul>
 *
 * <p>The URL is never logged by callers.</p>
 */
@Component
public class PlaybackUrlSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final String cdnBaseUrl;
    private final byte[] secret;
    private final ObjectProvider<ObjectStorageClient> storageProvider;

    public PlaybackUrlSigner(
            @Value("${vyroflix.playback.cdn-base-url:}") String cdnBaseUrl,
            @Value("${vyroflix.playback.signing-secret:}") String signingSecret,
            ObjectProvider<ObjectStorageClient> storageProvider) {
        this.cdnBaseUrl = stripTrailingSlash(cdnBaseUrl == null ? "" : cdnBaseUrl.trim());
        String effectiveSecret = signingSecret == null ? "" : signingSecret;
        if (!this.cdnBaseUrl.isEmpty() && effectiveSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "vyroflix.playback.signing-secret must be at least " + MIN_SECRET_LENGTH
                            + " characters when vyroflix.playback.cdn-base-url is set");
        }
        this.secret = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        this.storageProvider = storageProvider;
    }

    /**
     * @param masterPlaylistKey object key of the master playlist, e.g. {@code hls/titles/{id}/master.m3u8}
     * @param ttl               how long the manifest URL itself stays valid
     */
    public String sign(String masterPlaylistKey, Duration ttl) {
        if (cdnBaseUrl.isEmpty()) {
            return presign(masterPlaylistKey, ttl);
        }
        long exp = Instant.now().plus(ttl).getEpochSecond();
        return cdnBaseUrl + "/" + masterPlaylistKey
                + "?exp=" + exp
                + "&sig=" + signature(scopeOf(masterPlaylistKey), exp, secret);
    }

    /**
     * The asset folder one token authorises: the first two path segments plus a trailing slash, i.e.
     * {@code titles/{id}/} as written by Encoding-Service. Narrow on purpose: a token never covers other titles.
     * Must match the Worker's {@code scopeOf} exactly.
     */
    static String scopeOf(String objectKey) {
        String[] parts = objectKey.split("/");
        if (parts.length < 3 || parts[0].isEmpty() || parts[1].isEmpty() || objectKey.contains("..")) {
            throw new IllegalArgumentException("Unexpected encoded asset key layout");
        }
        return parts[0] + "/" + parts[1] + "/";
    }

    /** HMAC-SHA256 over {@code scope + "\n" + exp}, base64url without padding. */
    static String signature(String scope, long exp, byte[] secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] raw = mac.doFinal((scope + "\n" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC signing unavailable", e);
        }
    }

    private String presign(String key, Duration ttl) {
        ObjectStorageClient storage = storageProvider.getIfAvailable();
        if (storage == null) {
            // Local development without an object store bean.
            return "http://localhost:9000/" + Constants.BUCKET_CONTENT + "/" + key;
        }
        return storage.generatePresignedDownloadUrl(Constants.BUCKET_CONTENT, key, ttl);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
