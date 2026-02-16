package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AvroTransformerTest {
    
    private AvroTransformer transformer;
    
    @BeforeEach
    public void setUp() {
        transformer = new AvroTransformer();
    }
    
    @Test
    public void testTransformValidRecord() {
        // Arrange
        DataRecord record = DataRecord.builder()
                .id("test-avro-001")
                .sequenceNumber(4L)
                .fields(Map.of("col1", "val1", "col2", "val2"))
                .build();
        
        // Act
        Object result = transformer.transform(record);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("test-avro-001", map.get("id"));
        assertEquals(4L, map.get("sequence_number"));
    }
    
    @Test
    public void testTransformerName() {
        assertEquals("Avro/CQL Transformer", transformer.getName());
    }
}
