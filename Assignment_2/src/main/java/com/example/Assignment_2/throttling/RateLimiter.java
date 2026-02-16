package com.example.Assignment_2.throttling;

import lombok.RequiredArgsConstructor;

/**
 * Token-bucket rate limiter implementation.
 * Allows for smooth rate limiting with burst capacity.
 */
@RequiredArgsConstructor
public class RateLimiter {
    
    private final long tokensPerSecond;
    private final long maxBurstSize;
    private volatile long lastRefillTime = System.nanoTime();
    private volatile double availableTokens;
    
    public RateLimiter(long tokensPerSecond) {
        this.tokensPerSecond = tokensPerSecond;
        this.maxBurstSize = Math.max(tokensPerSecond, 10); // At least 10 tokens burst
        this.availableTokens = maxBurstSize;
    }
    
    /**
     * Acquires a token for processing. Blocks if no tokens available.
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public synchronized void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            long waitTime = calculateWaitTime();
            Thread.sleep(Math.min(waitTime / 1_000_000, 1)); // Sleep max 1ms at a time
        }
    }
    
    /**
     * Tries to acquire a token without blocking.
     * @return true if token acquired, false otherwise
     */
    public synchronized boolean tryAcquire() {
        refillTokens();
        
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        
        return false;
    }
    
    /**
     * Tries to acquire multiple tokens without blocking.
     * @param tokens Number of tokens to acquire
     * @return true if all tokens acquired, false otherwise
     */
    public synchronized boolean tryAcquire(int tokens) {
        refillTokens();
        
        if (availableTokens >= tokens) {
            availableTokens -= tokens;
            return true;
        }
        
        return false;
    }
    
    private void refillTokens() {
        long now = System.nanoTime();
        long timePassed = now - lastRefillTime;
        double tokensToAdd = (timePassed * tokensPerSecond) / 1_000_000_000.0;
        
        availableTokens = Math.min(maxBurstSize, availableTokens + tokensToAdd);
        lastRefillTime = now;
    }
    
    private long calculateWaitTime() {
        long now = System.nanoTime();
        long timeSinceLastRefill = now - lastRefillTime;
        long timeToNextToken = (long)((1.0 - availableTokens) * 1_000_000_000.0 / tokensPerSecond);
        return Math.max(0, timeToNextToken - timeSinceLastRefill);
    }
}
