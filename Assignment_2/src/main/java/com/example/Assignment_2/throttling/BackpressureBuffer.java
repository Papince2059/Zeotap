package com.example.Assignment_2.throttling;

import com.example.Assignment_2.model.SinkEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Backpressure handler using BlockingQueue to prevent memory overflow.
 * Implements bounded queue with configurable capacity.
 */
@Slf4j
public class BackpressureBuffer {
    
    private final BlockingQueue<SinkEvent> queue;
    private final int capacity;
    private final String name;
    
    public BackpressureBuffer(int capacity, String name) {
        this.capacity = capacity;
        this.name = name;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    /**
     * Puts an event into the queue, blocking if queue is full.
     * @param event The event to add
     * @throws InterruptedException if interrupted while waiting
     */
    public void put(SinkEvent event) throws InterruptedException {
        if (!queue.offer(event, 30, TimeUnit.SECONDS)) {
            throw new InterruptedException("Timeout: buffer full for " + name);
        }
    }
    
    /**
     * Tries to put an event without blocking.
     * @param event The event to add
     * @return true if added, false if queue is full
     */
    public boolean tryPut(SinkEvent event) {
        return queue.offer(event);
    }
    
    /**
     * Takes an event from the queue, blocking if empty.
     * @return The next event in the queue
     * @throws InterruptedException if interrupted while waiting
     */
    public SinkEvent take() throws InterruptedException {
        return queue.take();
    }
    
    /**
     * Polls an event with timeout.
     * @param timeout Time to wait
     * @param unit Time unit
     * @return The event, or null if timeout expired
     * @throws InterruptedException if interrupted
     */
    public SinkEvent poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }
    
    /**
     * Returns the current size of the queue.
     * @return Queue size
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Returns the remaining capacity.
     * @return Remaining capacity
     */
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }
    
    /**
     * Returns whether the queue is empty.
     * @return true if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Clears the queue.
     */
    public void clear() {
        queue.clear();
    }
}
