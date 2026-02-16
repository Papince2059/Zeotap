package com.example.Assignment_2.ingestion;

import com.example.Assignment_2.model.DataRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Factory for creating appropriate file readers based on file type.
 * Uses strategy pattern to handle different file formats.
 */
@Slf4j
@Component
public class FileReaderFactory {
    
    private final List<FileReader> readers;
    
    @Autowired
    public FileReaderFactory(JsonlFileReader jsonlReader, 
                            CsvFileReader csvReader,
                            FixedWidthFileReader fixedWidthReader) {
        this.readers = Arrays.asList(jsonlReader, csvReader, fixedWidthReader);
    }
    
    /**
     * Creates a reader for the given file path.
     * @param filePath Path to the file
     * @return Appropriate FileReader implementation
     * @throws IllegalArgumentException if no suitable reader found
     */
    public FileReader getReader(String filePath) {
        return readers.stream()
                .filter(reader -> reader.canHandle(filePath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "No suitable reader found for file: " + filePath));
    }
    
    /**
     * Reads records from the specified file using the appropriate reader.
     * @param filePath Path to the file
     * @return Stream of DataRecord objects
     */
    public Stream<DataRecord> readRecords(String filePath) {
        FileReader reader = getReader(filePath);
        log.info("Using reader: {} for file: {}", reader.getClass().getSimpleName(), filePath);
        return reader.readRecords(filePath);
    }
}
