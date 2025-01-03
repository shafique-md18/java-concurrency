package blockingqueue;

import java.util.concurrent.TimeoutException;

public interface IBlockingQueue<T> {
    void put(T item) throws InterruptedException;
    T take() throws InterruptedException;
    T take(long timeoutInMillis) throws InterruptedException, TimeoutException;
    T peek();
    boolean isEmpty();
    int getSize();
    int getCapacity();
}
