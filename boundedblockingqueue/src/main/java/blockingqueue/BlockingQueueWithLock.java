package blockingqueue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueWithLock<T> implements IBlockingQueue<T> {
    private int size;
    private final int capacity;
    private final Queue<T> queue;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    /*
    When to use Conditions:
        1. When you need multiple wait/notify scenarios
        2. When you need fairness guarantees
        3. When you need timeout capabilities
        4. High contention scenarios
     */

    public BlockingQueueWithLock(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity cannot be 0");
        }
        this.queue = new LinkedList<>();
        this.capacity = capacity;
        this.size = 0;
    }

    @Override
    public void put(T item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException("Cannot add null item to queue");
        }
        lock.lock(); // Like entering synchronized block
        try {
            while (this.size == this.capacity) {
                // Intuition: We want the queue to be "notFull" when we put an item
                notFull.await(); // Like wait() - wait until queue isn't full
            }
            queue.add(item);
            this.size++;
            System.out.println("Added item to the queue: " + item);
            notEmpty.signal(); // Like notify() - tell consumers there's an item
        } finally {
            lock.unlock(); // ALWAYS unlock in finally
        }
    }

    @Override
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (this.size == 0) {
                System.out.println("Queue is empty, waiting until at-least one element is present");
                // Intuition: We want the queue to be notEmpty when we want to take an item
                notEmpty.await(); // Wait until queue isn't empty
            }
            T item = queue.remove();
            this.size--;
            System.out.println("Removed item from the queue: " + item);
            notFull.signal(); // Tell producers there's space
            return item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T take(long timeoutInMillis) throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            long endTime = System.currentTimeMillis() + timeoutInMillis;
            while (this.size == 0) {
                System.out.println("Queue is empty, waiting until at-least one element is present");
                long remainingTime = endTime - System.currentTimeMillis();
                if (remainingTime <= 0) {
                    throw new TimeoutException("Timeout waiting for item from queue");
                }
                boolean signal = notEmpty.await(remainingTime, TimeUnit.MILLISECONDS);
                if (!signal && this.size == 0) {
                    throw new TimeoutException("Timeout waiting for item from queue");
                }
            }
            T item = queue.remove();
            this.size--;
            System.out.println("Removed item from the queue: " + item);
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    // non-blocking peek
    @Override
    public T peek() {
        lock.lock();
        try {
            if (isEmpty()) {
                return null;
            }
            return queue.peek();
        } finally {
            lock.unlock();
        }
    }

    // Simple getter, no need for locking as capacity is final
    @Override
    public int getCapacity() {
        return this.capacity;
    }

    // keeping the below methods lock to maintain high consistency
    // remove lock if you need performance over consistency
    @Override
    public int getSize() {
        lock.lock();
        try {
            return this.size;
        } finally {
            lock.unlock();
        }
    }

    // Use lock instead of synchronized
    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return this.size == 0;
        } finally {
            lock.unlock();
        }
    }
}
