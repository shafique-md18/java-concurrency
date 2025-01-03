package ratelimiter;

import ratelimiter.factory.RateLimiterFactory;

public class Main {
    public static void main(String[] args) {
        RateLimiterConfig config = new RateLimiterConfig.Builder()
                .withMaxRequests(5)
                .withWindowSizeInMillis(1000)
                .withRateLimiterType(RateLimiterType.TOKEN_BUCKET)
                .build();

        IRateLimiter limiter = RateLimiterFactory.getRateLimiter(config);

        // Create threads that use tryAcquire (non-blocking)
        Thread[] nonBlockingThreads = createThreads(5, limiter, true);
        // Create threads that use acquire (blocking)
        Thread[] blockingThreads = createThreads(5, limiter, false);

        // Start all threads
        startThreads(nonBlockingThreads, "Non-blocking");
        startThreads(blockingThreads, "Blocking");

        // Wait for all threads to complete
        waitForThreads(nonBlockingThreads);
        waitForThreads(blockingThreads);
    }

    private static Thread[] createThreads(int count, IRateLimiter limiter, boolean useTryAcquire) {
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // Each thread makes 3 attempts
                    for (int j = 0; j < 3; j++) {
                        if (useTryAcquire) {
                            boolean acquired = limiter.tryAcquire();
                            System.out.println("Thread " + threadId + " (tryAcquire) - attempt " + j +
                                    ": " + (acquired ? "succeeded" : "failed"));
                            if (!acquired) {
                                Thread.sleep(100); // Wait a bit before retrying
                                continue;
                            }
                        } else {
                            System.out.println("Thread " + threadId + " (acquire) attempting - attempt " + j);
                            limiter.acquire();
                            System.out.println("Thread " + threadId + " (acquire) succeeded - attempt " + j);
                        }

                        // Simulate work
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Thread " + threadId +
                            (useTryAcquire ? " (tryAcquire)" : " (acquire)") + " was interrupted");
                }
            });
        }
        return threads;
    }

    private static void startThreads(Thread[] threads, String type) {
        System.out.println("Starting " + type + " threads...");
        for (Thread thread : threads) {
            thread.start();
        }
    }

    private static void waitForThreads(Thread[] threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
