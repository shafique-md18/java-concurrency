package connectionpool;

import java.util.concurrent.TimeoutException;

public interface IConnectionPool {
    Connection getConnection() throws InterruptedException, TimeoutException;
    Connection getConnection(long timeout) throws InterruptedException, TimeoutException;
    void releaseConnection(Connection connection) throws InterruptedException;
}
