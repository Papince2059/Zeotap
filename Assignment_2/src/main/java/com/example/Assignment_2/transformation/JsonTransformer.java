package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms data records to JSON format.
 * Used for REST API sinks that accept JSON payloads.
 */
@Slf4j
@Component
public class JsonTransformer implements Transformer {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Object transform(DataRecord record) {
        Map<String, Object> output = new HashMap<>();
        output.put("id", record.getId());
        output.put("sequenceNumber", record.getSequenceNumber());
        output.put("data", record.getFields());
        output.put("timestamp", System.currentTimeMillis());
        return output;
    }
    
    @Override
    public String getName() {
        return "JSON Transformer";
    }
}
