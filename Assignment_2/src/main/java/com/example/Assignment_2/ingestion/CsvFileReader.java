package com.example.Assignment_2.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.example.Assignment_2.exception.FanOutException;
import com.example.Assignment_2.model.DataRecord;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads CSV files with support for different delimiters and formats.
 */
@Slf4j
@Component
public class CsvFileReader implements FileReader {
    
    @Override
    public Stream<DataRecord> readRecords(String filePath) {
        try {
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8));
            
            return csvParser.stream()
                    .map(this::parseRecord)
                    .onClose(() -> {
                        try {
                            csvParser.close();
                        } catch (IOException e) {
                            log.error("Error closing CSV parser for {}", filePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new FanOutException("Failed to read CSV file: " + filePath, e);
        }
    }
    
    @Override
    public boolean canHandle(String filePath) {
        return filePath.toLowerCase().endsWith(".csv");
    }
    
    private DataRecord parseRecord(CSVRecord csvRecord) {
        Map<String, Object> fields = new HashMap<>();
        
        for (String header : csvRecord.getParser().getHeaderNames()) {
            try {
                fields.put(header, csvRecord.get(header));
            } catch (IllegalArgumentException e) {
                log.warn("Missing field: {}", header);
                fields.put(header, null);
            }
        }
        
        return DataRecord.builder()
                .fields(fields)
                .build();
    }
}
