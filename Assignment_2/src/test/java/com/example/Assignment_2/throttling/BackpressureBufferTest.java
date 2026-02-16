package com.example.Assignment_2.throttling;

import com.example.Assignment_2.model.DataRecord;
import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.SinkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BackpressureBufferTest {
    
    private BackpressureBuffer buffer;
    
    @BeforeEach
    public void setUp() {
        buffer = new BackpressureBuffer(10, "test-buffer");
    }
    
    @Test
    public void testPutAndTake() throws InterruptedException {
        // Arrange
        SinkEvent event = SinkEvent.builder()
                .eventId("test-1")
                .sinkType(SinkType.REST_API)
                .build();
        
        // Act
        buffer.put(event);
        
        // Assert
        assertEquals(1, buffer.size());
        
        SinkEvent retrieved = buffer.take();
        assertEquals("test-1", retrieved.getEventId());
        assertEquals(0, buffer.size());
    }
    
    @Test
    public void testTryPut() {
        SinkEvent event = SinkEvent.builder()
                .eventId("test-2")
                .sinkType(SinkType.GRPC)
                .build();
        
        assertTrue(buffer.tryPut(event));
        assertEquals(1, buffer.size());
    }
    
    @Test
    public void testBufferCapacity() {
        // Fill up the buffer
        for (int i = 0; i < 10; i++) {
            SinkEvent event = SinkEvent.builder()
                    .eventId("event-" + i)
                    .sinkType(SinkType.MESSAGE_QUEUE)
                    .build();
            assertTrue(buffer.tryPut(event));
        }
        
        assertEquals(10, buffer.size());
        assertEquals(0, buffer.remainingCapacity());
        
        // Next should fail
        SinkEvent event = SinkEvent.builder()
                .eventId("overflow")
                .sinkType(SinkType.WIDE_COLUMN_DB)
                .build();
        assertFalse(buffer.tryPut(event));
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(buffer.isEmpty());
        
        SinkEvent event = SinkEvent.builder()
                .eventId("test-3")
                .sinkType(SinkType.REST_API)
                .build();
        
        buffer.tryPut(event);
        assertFalse(buffer.isEmpty());
    }
}
