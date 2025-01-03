package ratelimiter.factory;

import ratelimiter.*;

public class RateLimiterFactory {
    public static IRateLimiter getRateLimiter(RateLimiterConfig config) {
        return switch (config.getRateLimiterType()) {
            case FIXED_WINDOW -> new FixedWindowRateLimiter(config);
            case SLIDING_WINDOW -> new SlidingWindowRateLimiter(config);
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(config);
            default -> throw new IllegalArgumentException("Invalid type of rate limiter");
        };
    }
}
