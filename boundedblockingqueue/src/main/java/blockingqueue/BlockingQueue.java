package blockingqueue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

/**
 * First Solution (synchronized with wait()/notifyAll()) is simpler, faster, and more efficient for this case.
 * Since the queue is used with exclusive access for enqueue and dequeue operations, there's no benefit to using read-write locks.
 * The simplicity of synchronized is effective here and leads to better performance.
 * <p>
 * Second Solution (ReentrantReadWriteLock with Condition) is overengineered for this specific problem.
 * It introduces unnecessary complexity and overhead, particularly because we are not dealing with a situation where multiple readers
 * and few writers benefit from read-write locks. It is better suited for cases where there are frequent reads and infrequent writes, but this doesn't align well with the bounded blocking queue problem.
 * <p>
 * For bounded blocking queues, synchronized with wait() and notifyAll() is the better approach, resulting in cleaner
 * and more efficient code for the problem at hand.
 */

public class BlockingQueue<T> implements IBlockingQueue<T> {
    // Should fields be volatile?
    // No need with synchronized methods as they provide happens-before guarantees.
    private int size;
    private final int capacity;
    private final Queue<T> queue;

    public BlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity cannot be 0");
        }
        this.queue = new LinkedList<>();
        this.capacity = capacity;
        this.size = 0;
    }

    public synchronized void put(T item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException("Cannot add null item to queue");
        }
        while (this.size == this.capacity) {
            System.out.println("Queue is full, waiting until capacity is available");
            try {
                wait();
            } catch (InterruptedException e) {
                notifyAll(); // Signal other waiting threads before leaving
                throw e;
            }
        }
        queue.add(item);
        this.size++;
        System.out.println("Added item to the queue: " + item);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        /*
          Why use while?
            Because of spurious wake-ups - threads might wake up even without notify().
            Also, after waking up, another thread might have taken the last item,
            so we need to recheck.
         */
        while (this.size == 0) {
            System.out.println("Queue is empty, waiting until atleast one element is present");
            try {
                wait();
            } catch (InterruptedException e) {
                notifyAll(); // Signal other waiting threads before leaving
                throw e;
            }
        }
        T item = queue.remove();
        this.size--;
        System.out.println("Removed item from the queue: " + item);
        /*
          Why use notify() instead of notifyAll()?
            notify() wakes up one thread - more efficient but might cause starvation
            notifyAll() wakes up all threads - less efficient but fairer
            If multiple producers/consumers, notifyAll() is often better
         */
        notifyAll();
        return item;
    }

    @Override
    public T take(long timeoutInMillis) throws InterruptedException, TimeoutException {
        // TODO: Implement
        return null;
    }

    public synchronized T peek() {
        if (isEmpty()) {
            return null;
        }
        return queue.peek();
    }

    public synchronized int getSize() {  return this.size; }

    public synchronized int getCapacity() { return this.capacity; }

    public synchronized boolean isEmpty() { return this.size == 0; }
}
