package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Transforms data records to Protobuf format.
 * Used for gRPC sinks that use Protocol Buffers for serialization.
 * 
 * Note: This is a simplified implementation. In production, you would generate
 * proper Protobuf classes using protoc compiler.
 */
@Slf4j
@Component
public class ProtobufTransformer implements Transformer {
    
    @Override
    public Object transform(DataRecord record) {
        // In production, you would use generated Protobuf classes
        // For this demo, we'll create a serializable representation
        Map<String, Object> protobufFields = new HashMap<>();
        protobufFields.put("id", record.getId());
        protobufFields.put("sequence_number", record.getSequenceNumber());
        protobufFields.put("fields", record.getFields());
        protobufFields.put("timestamp", System.currentTimeMillis());
        
        // Return as string representation for demo
        return protobufFields.toString().getBytes();
    }
    
    @Override
    public String getName() {
        return "Protobuf Transformer";
    }
}
