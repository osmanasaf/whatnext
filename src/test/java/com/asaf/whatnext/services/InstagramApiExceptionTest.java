package com.asaf.whatnext.services;

import com.asaf.whatnext.services.InstagramService.InstagramApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstagramApiExceptionTest {

    private static InstagramApiException error(String body) {
        return new InstagramApiException(body, 400);
    }

    /** Canlı gövde (2026-09-12 story reddi) — rate limit sayılıp 15 dk uyutulmamalı. */
    @Test
    void mediaDownloadFailure9004_isNotRateLimit() {
        InstagramApiException e = error("{\"error\":{\"message\":\"Only photo or video can be accepted as media type.\","
                + "\"type\":\"OAuthException\",\"code\":9004,\"error_subcode\":2207052,\"is_transient\":false}}");

        assertThat(e.isRateLimitError()).isFalse();
        assertThat(e.isActionBlockedError()).isFalse();
        assertThat(e.isExpiredOrInvalidToken()).isFalse();
        assertThat(e.isMediaFetchError()).isTrue();
        assertThat(e.getMessage()).contains("code=9004").contains("subcode=2207052");
        assertThat(error("{\"error\":{\"code\":4}}").isMediaFetchError()).isFalse();
    }

    @Test
    void realLimits_areRateLimit() {
        assertThat(error("{\"error\":{\"code\":4,\"message\":\"Application request limit reached\"}}").isRateLimitError()).isTrue();
        assertThat(error("{\"error\":{\"code\":9,\"error_subcode\":2207042}}").isRateLimitError()).isTrue();
        assertThat(error("{\"error\":{\"code\":40}}").isRateLimitError()).isFalse();
    }

    @Test
    void blockedAndToken() {
        assertThat(error("{\"error\":{\"code\":9,\"error_subcode\":2207051}}").isActionBlockedError()).isTrue();
        assertThat(error("{\"error\":{\"code\":190,\"error_subcode\":463}}").isExpiredOrInvalidToken()).isTrue();
        assertThat(error("{\"error\":{\"code\":1900}}").isExpiredOrInvalidToken()).isFalse();
    }

    @Test
    void malformedBody_isNothing() {
        InstagramApiException e = error("<html>Bad Gateway</html>");
        assertThat(e.isRateLimitError()).isFalse();
        assertThat(e.isExpiredOrInvalidToken()).isFalse();
        assertThat(new InstagramApiException(null, 502).getMessage()).contains("502");
    }
}
