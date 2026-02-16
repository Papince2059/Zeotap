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

import lombok.extern.slf4j.Slf4j;

/**
 * Reads fixed-width format files.
 * Column widths should be specified in a configuration file or format specification.
 */
@Slf4j
@Component
public class FixedWidthFileReader implements FileReader {
    
    public Stream<DataRecord> readRecords(String filePath) {
        return readRecords(filePath, null);
    }
    
    /**
     * Reads fixed-width file with specified column widths.
     * @param filePath Path to the file
     * @param columnWidths Array of column widths
     * @return Stream of DataRecord objects
     */
    public Stream<DataRecord> readRecords(String filePath, int[] columnWidths) {
        try {
            BufferedReader reader = new BufferedReader(
                new java.io.FileReader(filePath, StandardCharsets.UTF_8),
                65536  // 64KB buffer
            );
            
            // If no column widths provided, try to infer from first line
            final int[] widths = columnWidths != null ? columnWidths : inferColumnWidths(reader, filePath);
            
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> parseFixedWidthLine(line, widths))
                    .onClose(() -> {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            log.error("Error closing reader for {}", filePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new FanOutException("Failed to read fixed-width file: " + filePath, e);
        }
    }
    
    @Override
    public boolean canHandle(String filePath) {
        return filePath.toLowerCase().endsWith(".txt") || 
               filePath.toLowerCase().endsWith(".fixed");
    }
    
    private int[] inferColumnWidths(BufferedReader reader, String filePath) throws IOException {
        String firstLine = reader.readLine();
        if (firstLine == null) {
            throw new FanOutException("Empty file: " + filePath);
        }
        
        // For demo, split by consistent spacing (at least 2+ spaces)
        String[] parts = firstLine.trim().split("  +");
        return new int[parts.length];
    }
    
    private DataRecord parseFixedWidthLine(String line, int[] columnWidths) {
        Map<String, Object> fields = new HashMap<>();
        int startIndex = 0;
        
        for (int i = 0; i < columnWidths.length && startIndex < line.length(); i++) {
            int endIndex = Math.min(startIndex + columnWidths[i], line.length());
            String value = line.substring(startIndex, endIndex).trim();
            fields.put("Column_" + (i + 1), value);
            startIndex = endIndex;
        }
        
        return DataRecord.builder()
                .fields(fields)
                .build();
    }
}
