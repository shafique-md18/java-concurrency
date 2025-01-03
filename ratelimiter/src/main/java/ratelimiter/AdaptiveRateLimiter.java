package ratelimiter;

public class AdaptiveRateLimiter implements IRateLimiter {
    @Override
    public boolean tryAcquire() {
        return false;
    }

    @Override
    public void acquire() {

    }
}
