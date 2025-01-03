package ratelimiter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindowRateLimiter implements IRateLimiter {
    private RateLimiterConfig config;
    private Queue<Long> requestTimestamps;
    private ReentrantLock lock;
    private Condition notLimited;

    public SlidingWindowRateLimiter(RateLimiterConfig config) {
        this.config = config;
        this.requestTimestamps = new LinkedList<>();
        this.lock = new ReentrantLock(true);
        this.notLimited = lock.newCondition();
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

    private boolean attemptAcquire() {
        long currentTime = System.currentTimeMillis();
        // Window start time is dynamic
        long windowStart = currentTime - config.getWindowSizeInMillis();
        while (!this.requestTimestamps.isEmpty() && this.requestTimestamps.peek() <= windowStart) {
            this.requestTimestamps.remove();
            this.notLimited.signalAll();
        }

        if (this.requestTimestamps.size() < config.getMaxRequests()) {
            requestTimestamps.add(currentTime);
            return true;
        }

        return false;
    }

    @Override
    public void acquire() throws InterruptedException {
        this.lock.lockInterruptibly();
        try {
            while (!attemptAcquire()) {
                Long oldestTimestamp = requestTimestamps.peek();
                if (oldestTimestamp == null) {
                    continue;
                }
                long waitTime = oldestTimestamp + config.getWindowSizeInMillis() - System.currentTimeMillis();
                if (waitTime <= 0) {
                    continue;
                }
                System.out.println("Waiting for: " + waitTime + "ms");
                if (!notLimited.await(waitTime, TimeUnit.MILLISECONDS)) {
                    System.out.println("Await timeout, retrying...");
                }
            }
        } finally {
            this.lock.unlock();
        }
    }
}
