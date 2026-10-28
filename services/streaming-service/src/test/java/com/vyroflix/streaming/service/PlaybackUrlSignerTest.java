package com.vyroflix.streaming.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PlaybackUrlSignerTest {

    private static final String SECRET = "unit-test-secret-with-more-than-32-chars";
    private static final String KEY = "titles/3f2b1c00-0000-4000-8000-000000000001/hls/master.m3u8";

    @Test
    @DisplayName("CDN mode: the URL carries an expiry and an HMAC over the asset folder, verifiable independently")
    @SuppressWarnings("unchecked")
    void cdnUrlIsVerifiable() throws Exception {
        PlaybackUrlSigner signer = new PlaybackUrlSigner("https://media.example/", SECRET, mock(ObjectProvider.class));

        String url = signer.sign(KEY, Duration.ofMinutes(15));

        Map<String, String> query = parseQuery(url);
        long exp = Long.parseLong(query.get("exp"));
        assertThat(url).startsWith("https://media.example/" + KEY + "?");
        assertThat(exp).isGreaterThan(System.currentTimeMillis() / 1000);
        assertThat(query.get("sig")).isEqualTo(expectedSignature("titles/3f2b1c00-0000-4000-8000-000000000001/", exp));
    }

    @Test
    @DisplayName("scope is the asset folder, so a token never covers other titles")
    void scopeIsAssetFolder() {
        assertThat(PlaybackUrlSigner.scopeOf(KEY)).isEqualTo("titles/3f2b1c00-0000-4000-8000-000000000001/");
    }

    @Test
    @DisplayName("keys that try to escape the asset folder are rejected")
    void rejectsTraversal() {
        assertThatThrownBy(() -> PlaybackUrlSigner.scopeOf("titles/../../secrets/master.m3u8"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaybackUrlSigner.scopeOf("master.m3u8"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CDN mode refuses to start with a short signing secret")
    @SuppressWarnings("unchecked")
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new PlaybackUrlSigner("https://media.example", "short", mock(ObjectProvider.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("signing-secret");
    }

    @Test
    @DisplayName("tokens for one asset do not verify against another asset's scope")
    void tokenIsScoped() throws Exception {
        long exp = System.currentTimeMillis() / 1000 + 600;
        String forOther = expectedSignature("titles/other-title/", exp);
        assertThat(forOther).isNotEqualTo(expectedSignature("titles/3f2b1c00-0000-4000-8000-000000000001/", exp));
    }

    @Test
    @DisplayName("signature matches the Cloudflare Worker vector (cross-language contract)")
    void matchesWorkerVector() {
        // Same value asserted in infra/cloudflare/hls-worker/test/worker.test.mjs.
        assertThat(PlaybackUrlSigner.signature("titles/3f2b1c00-0000-4000-8000-000000000001/", 1_900_000_000L,
                "test-secret-that-is-at-least-32-characters-long".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("_Wq88MfLUKJ_ycEeq8GtYbSR3mhwhI7-2l28_CbltGU");
    }

    private static String expectedSignature(String scope, long exp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal((scope + "\n" + exp).getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static Map<String, String> parseQuery(String url) {
        String query = url.substring(url.indexOf('?') + 1);
        return java.util.Arrays.stream(query.split("&"))
                .map(p -> p.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(p -> p[0], p -> p[1]));
    }
}
