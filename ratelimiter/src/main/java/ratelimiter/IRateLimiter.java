package ratelimiter;

public interface IRateLimiter {
    boolean tryAcquire();
    void acquire() throws InterruptedException;
}
