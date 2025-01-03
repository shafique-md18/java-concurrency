package connectionpool;

import blockingqueue.BlockingQueueWithLock;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class ConnectionPool implements IConnectionPool {
    private final BlockingQueueWithLock<Connection> queue;
    private final int maxConnections;
    private volatile boolean isShutdown;

    public ConnectionPool(Builder builder) throws InterruptedException {
        this.maxConnections = builder.maxConnections;
        queue = new BlockingQueueWithLock<>(maxConnections);
        initializeConnections();
    }

    private void initializeConnections() throws InterruptedException {
        for (int i = 0; i < maxConnections; i++) {
            queue.put(newConnection());
        }
    }

    private Connection newConnection() {
        return new Connection(UUID.randomUUID().toString());
    }

    @Override
    public Connection getConnection() throws InterruptedException, TimeoutException {
        return getConnection(queue.getCapacity());
    }

    private Connection getConnection(int maxRetries) throws InterruptedException, TimeoutException {
        if (maxRetries == 0) {
            throw new RuntimeException(String.format("Failed to get valid connection after %d attempts",
                    queue.getCapacity()));
        }
        checkIfShutdown();
        Connection conn = queue.take();

        if (!conn.isValid()) {
            System.out.println("Invalid connection found, putting back new connection in queue");
            Connection newConn = newConnection();
            queue.put(newConn);
            return getConnection(maxRetries - 1);
        }

        return conn;
    }

    @Override
    public Connection getConnection(long timeout) throws InterruptedException, TimeoutException {
        if (timeout <= 0) {
            throw new TimeoutException("Timeout while waiting for connection.");
        }
        long endTime = System.currentTimeMillis() + timeout;
        checkIfShutdown();
        Connection conn = queue.take();

        if (!conn.isValid()) {
            Connection newConn = newConnection();
            queue.put(newConn);
            long remainingTime = endTime - System.currentTimeMillis();
            return getConnection(remainingTime);
        }

        return conn;
    }

    @Override
    public void releaseConnection(Connection conn) throws InterruptedException {
        if (conn != null && conn.isValid()) {
            System.out.println("Released connection is valid, putting same connection back in queue");
            queue.put(conn);
        } else {
            // If the existing conn is no longer valid create a new replacement conn
            System.out.println("Released connection is invalid, putting new connection back in queue");
            queue.put(newConnection());
        }
    }

    public int getAvailableConnectionsCount() {
        return queue.getSize();
    }

    public void shutdown() throws InterruptedException {
        isShutdown = true;
        while (!queue.isEmpty()) {
            Connection conn = queue.take();
            conn.close();
        }
    }

    private void checkIfShutdown() {
        if (isShutdown) {
            throw new IllegalStateException("Trying to access connection pool, but is shutdown");
        }
    }

    public static class Builder {
        private BlockingQueueWithLock<Connection> queue;
        private int maxConnections = 10;
        private long defaultTimeout = 5000;

        public Builder withMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder withDefaultTimeout(int defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
            return this;
        }

        public ConnectionPool build() throws InterruptedException {
            validate();
            return new ConnectionPool(this);
        }

        private void validate() {
            if (maxConnections <= 0) {
                throw new IllegalArgumentException("Pool size must be positive");
            }
            if (defaultTimeout <= 0) {
                throw new IllegalArgumentException("Pool default timeout must be positive");
            }
        }
    }
}
