package ratelimiter;

public class LeakyBucketRateLimiter implements IRateLimiter{
    @Override
    public boolean tryAcquire() {
        return false;
    }

    @Override
    public void acquire() {

    }
}
