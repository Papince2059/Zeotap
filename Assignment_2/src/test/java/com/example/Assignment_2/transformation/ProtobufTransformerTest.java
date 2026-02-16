package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProtobufTransformerTest {
    
    private ProtobufTransformer transformer;
    
    @BeforeEach
    public void setUp() {
        transformer = new ProtobufTransformer();
    }
    
    @Test
    public void testTransformValidRecord() {
        // Arrange
        DataRecord record = DataRecord.builder()
                .id("test-789")
                .sequenceNumber(3L)
                .fields(Map.of("field1", "value1", "field2", "value2"))
                .build();
        
        // Act
        Object result = transformer.transform(record);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        byte[] bytes = (byte[]) result;
        assertTrue(bytes.length > 0);
    }
    
    @Test
    public void testTransformerName() {
        assertEquals("Protobuf Transformer", transformer.getName());
    }
}
