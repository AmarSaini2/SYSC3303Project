import java.util.LinkedList;
import java.util.Queue;

public class GenericQueue<T> {
    private final Queue<T> queue;

    /**
     * Constructor initializes the queue.
     */
    public GenericQueue() {
        this.queue = new LinkedList<>();
    }

    /**
     * Adds an item to the queue and notifies waiting threads.
     * 
     * @param item The item to add.
     */
    public synchronized void add(T item) {
        queue.add(item);
        notifyAll(); // Wake up any waiting threads
        System.out.println("[GenericQueue] Added: " + item);
    }

    /**
     * Retrieves and removes the next available item from the queue.
     * If the queue is empty, it waits until an item is available.
     * 
     * @return The next item from the queue.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public synchronized T get() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // Wait for an item to be added
        }
        return queue.poll();
    }

    /**
     * Retrieves an item if available, otherwise returns null (Non-blocking).
     * 
     * @return The next item, or null if the queue is empty.
     */
    public synchronized T getNonBlocking() {
        return queue.poll(); // Returns null if queue is empty
    }

    /**
     * Gets the current size of the queue.
     * 
     * @return The number of elements in the queue.
     */
    public synchronized int size() {
        return queue.size();
    }

    /**
     * Checks if the queue is empty.
     * 
     * @return true if empty, false otherwise.
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
