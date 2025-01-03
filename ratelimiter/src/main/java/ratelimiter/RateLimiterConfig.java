package ratelimiter;

public class RateLimiterConfig {
    private RateLimiterType rateLimiterType;
    private int maxRequests;
    private long windowSizeInMillis;

    private RateLimiterConfig(Builder builder) {
        this.rateLimiterType = builder.rateLimiterType;
        this.maxRequests = builder.maxRequests;
        this.windowSizeInMillis = builder.windowSizeInMillis;
    }

    public RateLimiterType getRateLimiterType() {
        return rateLimiterType;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindowSizeInMillis() {
        return windowSizeInMillis;
    }

    public static class Builder {
        private RateLimiterType rateLimiterType = RateLimiterType.FIXED_WINDOW;
        private int maxRequests;
        private long windowSizeInMillis;

        public Builder withRateLimiterType(RateLimiterType rateLimiterType) {
            this.rateLimiterType = rateLimiterType;
            return this;
        }

        public Builder withMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
            return this;
        }

        public Builder withWindowSizeInMillis(long windowSizeInMillis) {
            this.windowSizeInMillis = windowSizeInMillis;
            return this;
        }

        public RateLimiterConfig build() {
            return new RateLimiterConfig(this);
        }
    }
}
