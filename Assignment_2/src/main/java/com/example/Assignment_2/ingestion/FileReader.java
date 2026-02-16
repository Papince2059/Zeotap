package com.example.Assignment_2.ingestion;

import com.example.Assignment_2.model.DataRecord;
import java.util.stream.Stream;

/**
 * Interface for reading data records from various file formats.
 * Implementations must support streaming to handle large files without loading into memory.
 */
public interface FileReader {
    /**
     * Reads records from a file and returns them as a stream.
     * @param filePath Path to the file
     * @return Stream of DataRecord objects
     */
    Stream<DataRecord> readRecords(String filePath);
    
    /**
     * Checks if this reader can handle the given file type.
     * @param filePath Path to check
     * @return true if this reader can handle this file type
     */
    boolean canHandle(String filePath);
}
