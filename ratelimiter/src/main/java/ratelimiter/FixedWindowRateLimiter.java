package ratelimiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FixedWindowRateLimiter implements IRateLimiter{
    private final RateLimiterConfig config;
    private long windowStartTime;
    private int currentCount;
    private final ReentrantLock lock;
    private final Condition notLimited;

    public FixedWindowRateLimiter(RateLimiterConfig config) {
        this.config = config;
        currentCount = 0;
        lock = new ReentrantLock(true); // for fairness
        notLimited = lock.newCondition();
        windowStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean tryAcquire() {
        lock.lock();
        try {
            return attemptAcquire();
        } finally {
            lock.unlock();
        }
    }

    /*
      Why use lockInterruptibly?
        If we need to cancel/timeout a thread that's waiting for a rate limiter (like an API request timeout),
        lockInterruptibly() allows us to interrupt it, while lock() would keep the thread waiting indefinitely,
        potentially causing resource leaks and poor user experience.

     */
    @Override
    public void acquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (!attemptAcquire()) {
                long waitTime = windowStartTime + config.getWindowSizeInMillis() - System.currentTimeMillis();
                if (waitTime <= 0) {
                    System.out.println("Window elapsed, will attempt to acquire again");
                    continue;
                }
                System.out.println("Waiting for - " + waitTime);
                if (!notLimited.await(waitTime, TimeUnit.MILLISECONDS)) {
                    System.out.println("Timeout in current window, will attempt to acquire again");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean attemptAcquire() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - windowStartTime >= config.getWindowSizeInMillis()) {
            windowStartTime = currentTime;
            currentCount = 0;
            notLimited.signalAll();
        }

        if (currentCount < config.getMaxRequests()) {
            currentCount++;
            return true;
        }

        return false;
    }
}
