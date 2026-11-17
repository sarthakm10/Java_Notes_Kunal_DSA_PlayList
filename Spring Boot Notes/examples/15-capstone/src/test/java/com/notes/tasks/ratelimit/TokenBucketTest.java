package com.notes.tasks.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A plain unit test: no Spring needed for the algorithm itself. */
class TokenBucketTest {

    @Test
    void allowsABurstUpToCapacityThenAsksToWait() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(3, 0.5);   // 1 token per 2 s
        assertThat(bucket.tryTake()).isZero();
        assertThat(bucket.tryTake()).isZero();
        assertThat(bucket.tryTake()).isZero();
        assertThat(bucket.tryTake()).isBetween(1L, 2L);       // empty: wait about 2 s for the next token
    }
}
