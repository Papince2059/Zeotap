package com.example.Assignment_2.ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.example.Assignment_2.exception.FanOutException;
import com.example.Assignment_2.model.DataRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads JSONL (JSON Lines) files line by line.
 * Each line should be a valid JSON object.
 */
@Slf4j
@Component
public class JsonlFileReader implements FileReader {
    
    private final ObjectMapper objectMapper;
    
    public JsonlFileReader() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public Stream<DataRecord> readRecords(String filePath) {
        try {
            BufferedReader reader = new BufferedReader(
                new java.io.FileReader(filePath, StandardCharsets.UTF_8),
                65536  // 64KB buffer for better performance
            );
            
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> parseJsonLine(line, filePath))
                    .onClose(() -> {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            log.error("Error closing reader for {}", filePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new FanOutException("Failed to read JSONL file: " + filePath, e);
        }
    }
    
    @Override
    public boolean canHandle(String filePath) {
        return filePath.toLowerCase().endsWith(".jsonl") || 
               filePath.toLowerCase().endsWith(".ndjson");
    }
    
    private DataRecord parseJsonLine(String line, String filePath) {
        try {
            JsonNode node = objectMapper.readTree(line);
            Map<String, Object> fields = new HashMap<>();
            
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    fields.put(entry.getKey(), value.asText());
                } else if (value.isNumber()) {
                    fields.put(entry.getKey(), value.asText());
                } else {
                    fields.put(entry.getKey(), value.asText());
                }
            });
            
            return DataRecord.builder()
                    .fields(fields)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing JSON line from {}: {}", filePath, line, e);
            throw new FanOutException("Failed to parse JSON line", e);
        }
    }
}
