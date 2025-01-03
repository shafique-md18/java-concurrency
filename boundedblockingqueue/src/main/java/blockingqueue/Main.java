package blockingqueue;

public class Main {
    public static void main(String[] args) {
        testQueue(new BlockingQueueWithLock<>(3));
    }

    private static void testQueue(IBlockingQueue<Integer> queue) {
        int numProducers = 3;
        int numConsumers = 2;
        int itemsPerProducer = 5;

        // Create multiple producers
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            Thread producer = new Thread(() -> {
                try {
                    for (int j = 1; j <= itemsPerProducer; j++) {
                        Thread.sleep(3000);
                        int item = (producerId * 100) + j;  // Unique items per producer
                        System.out.println("Producer " + producerId + " trying to add: " + item);
                        queue.put(item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            producer.start();
        }

        // Create multiple consumers
        for (int i = 0; i < numConsumers; i++) {
            final int consumerId = i;
            Thread consumer = new Thread(() -> {
                try {
                    // Each consumer tries to take more items since we have more producers
                    for (int j = 1; j <= (itemsPerProducer * numProducers) / numConsumers; j++) {
                        System.out.println("Consumer " + consumerId + " trying to take item");
                        int item = queue.take();
                        System.out.println("Consumer " + consumerId + " got item: " + item);
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumer.start();
        }
    }
}