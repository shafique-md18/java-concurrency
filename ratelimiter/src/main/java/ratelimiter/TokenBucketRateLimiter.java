package ratelimiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter implements IRateLimiter {
    private double currentTokens;
    private long lastRefillTime;
    private RateLimiterConfig config;
    private ReentrantLock lock;
    private Condition notLimited;

    public TokenBucketRateLimiter(RateLimiterConfig config) {
        this.config = config;
        this.currentTokens = config.getMaxRequests();
        this.lock = new ReentrantLock(true);
        this.notLimited = this.lock.newCondition();
        this.lastRefillTime = System.nanoTime();
    }

    @Override
    public boolean tryAcquire() {
        this.lock.lock();
        try {
            return attemptAcquire();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void acquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (!attemptAcquire()) {
                // Calculate waiting time until next token
                double timeUntilNextToken = (1 - currentTokens) / getRefillRate();
                long waitTimeMillis = (long) (timeUntilNextToken * 1000);

                if (waitTimeMillis <= 0) {
                    continue;
                }
                System.out.println("Waiting for: " + waitTimeMillis + "ms");
                if (!notLimited.await(waitTimeMillis, TimeUnit.MILLISECONDS)) {
                    System.out.println("Await timeout, retrying...");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean attemptAcquire() {
        refillTokens();
        // consume a token
        if (currentTokens > 0) {
            currentTokens--;
            return true;
        }
        // no tokens available, request cannot proceed
        return false;
    }

    private void refillTokens() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillTime) / 1e9; // Convert nanos to seconds
        // tokens are refilled on per-second basis
        double tokensToAdd = elapsedSeconds * getRefillRate();

        currentTokens = Math.min(config.getMaxRequests(), currentTokens + tokensToAdd);
        lastRefillTime = now;
    }

    private double getRefillRate() {
        // Refill Rate = Max Requests / Time Window in Seconds
        // This tells how many tokens should be added per second, hence divided by 1000.0
        return config.getMaxRequests() / (config.getWindowSizeInMillis() / 1000.0);
    }
}
