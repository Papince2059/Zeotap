package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonTransformerTest {
    
    private JsonTransformer transformer;
    
    @BeforeEach
    public void setUp() {
        transformer = new JsonTransformer();
    }
    
    @Test
    public void testTransformValidRecord() {
        // Arrange
        DataRecord record = DataRecord.builder()
                .id("test-123")
                .sequenceNumber(1L)
                .fields(Map.of("name", "John", "age", "30"))
                .build();
        
        // Act
        Object result = transformer.transform(record);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("test-123", map.get("id"));
        assertEquals(1L, map.get("sequenceNumber"));
        assertNotNull(map.get("data"));
        assertNotNull(map.get("timestamp"));
    }
    
    @Test
    public void testTransformerName() {
        assertEquals("JSON Transformer", transformer.getName());
    }
    
    @Test
    public void testTransformEmptyRecord() {
        // Arrange
        DataRecord record = DataRecord.builder()
                .id("test-456")
                .sequenceNumber(2L)
                .fields(new HashMap<>())
                .build();
        
        // Act
        Object result = transformer.transform(record);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
    }
}
