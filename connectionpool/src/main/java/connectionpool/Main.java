package connectionpool;

import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        try {
            testConnectionPool();
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testConnectionPool() throws InterruptedException, TimeoutException {
        ConnectionPool pool = new ConnectionPool.Builder().withMaxConnections(3).build();

        int numThreads = 5;  // 5 threads trying to get connections
        Thread[] threads = new Thread[numThreads];

        // Create and start threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // Each thread tries to get and release connection multiple times
                    for (int j = 0; j < 3; j++) {
                        System.out.println("Thread " + threadId + " trying to get connection");

                        // Alternate between normal get and timeout get
                        Connection conn;
                        if (threadId % 2 == 0) {
                            conn = pool.getConnection();
                            System.out.println("Thread " + threadId + " got connection: normal get");
                        } else {
                            try {
                                conn = pool.getConnection(2000); // 2 second timeout
                                System.out.println("Thread " + threadId + " got connection: timeout get");
                            } catch (TimeoutException e) {
                                System.out.println("Thread " + threadId + " timeout waiting for connection");
                                continue;
                            }
                        }

                        // Simulate some work
                        Thread.sleep(1000);

                        // Occasionally invalidate connection
                        if (Math.random() < 0.3) {  // 30% chance
                            conn.close();
                            System.out.println("Thread " + threadId + " invalidated connection");
                        }

                        // Release connection
                        pool.releaseConnection(conn);
                        System.out.println("Thread " + threadId + " released connection");

                        // Print pool status
                        System.out.println("Available connections: " + pool.getAvailableConnectionsCount());
                    }
                } catch (InterruptedException e) {
                    System.err.println("Thread " + threadId + " interrupted: " + e.getMessage());
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }, "Worker-" + threadId);

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Connection Pool shutdown
        System.out.println("\nTesting shutdown...");
        pool.shutdown();

        // Try to get connection after shutdown (should throw exception)
        try {
            pool.getConnection();
            System.err.println("Error: Should not be able to get connection after shutdown");
        } catch (IllegalStateException e) {
            System.out.println("Successfully caught shutdown state: " + e.getMessage());
        }
    }
}