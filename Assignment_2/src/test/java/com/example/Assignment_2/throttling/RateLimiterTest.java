package com.example.Assignment_2.throttling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    public void setUp() {
        rateLimiter = new RateLimiter(10); // 10 tokens per second
    }
    
    @Test
    public void testAcquireSingleToken() throws InterruptedException {
        // Should succeed immediately
        long startTime = System.nanoTime();
        rateLimiter.acquire();
        long duration = System.nanoTime() - startTime;
        
        assertTrue(duration < 100_000_000); // Less than 100ms
    }
    
    @Test
    public void testTryAcquire() {
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }
        
        // Next should fail as tokens exhausted
        assertFalse(rateLimiter.tryAcquire());
    }
    
    @Test
    public void testTryAcquireMultiple() {
        assertTrue(rateLimiter.tryAcquire(5));
        assertTrue(rateLimiter.tryAcquire(5));
        assertFalse(rateLimiter.tryAcquire(2)); // Only 0 tokens left
    }
    
    @Test
    public void testRateLimiterWait() throws InterruptedException {
        // Use all tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }
        
        // This should block and wait
        Thread testThread = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                rateLimiter.acquire();
                long duration = System.currentTimeMillis() - startTime;
                assertTrue(duration >= 50); // Should wait at least 50ms (1 token at 10/sec)
            } catch (InterruptedException e) {
                fail("Thread was interrupted");
            }
        });
        
        testThread.start();
        testThread.join(2000);
    }
}
