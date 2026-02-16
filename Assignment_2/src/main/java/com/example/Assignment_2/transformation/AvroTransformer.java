package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms data records to Avro/CQL Map format.
 * Used for wide-column database sinks (Cassandra, Aerospike, DynamoDB, ScyllaDB).
 */
@Slf4j
@Component
public class AvroTransformer implements Transformer {
    
    @Override
    public Object transform(DataRecord record) {
        // Create a CQL-compatible map structure
        Map<String, Object> cqlMap = new HashMap<>();
        cqlMap.put("id", record.getId());
        cqlMap.put("sequence_number", record.getSequenceNumber());
        cqlMap.put("fields", record.getFields());
        cqlMap.put("timestamp", System.currentTimeMillis());
        
        // In production, this would be serialized to Avro binary format
        return cqlMap;
    }
    
    @Override
    public String getName() {
        return "Avro/CQL Transformer";
    }
}
