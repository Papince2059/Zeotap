package com.example.Assignment_2.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single data record from the source file.
 * Flexible schema to support CSV, JSON, and fixed-width data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataRecord {
    private String id;
    private long sequenceNumber;
    private Map<String, Object> fields = new HashMap<>();
    
    @JsonAnySetter
    public void set(String fieldName, Object value) {
        fields.put(fieldName, value);
    }
    
    public Object get(String fieldName) {
        return fields.get(fieldName);
    }
}
