package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;

/**
 * Strategy interface for transforming data records to various sink formats.
 */
public interface Transformer {
    /**
     * Transforms a data record to the target format.
     * @param record The source data record
     * @return Transformed data in the target format
     */
    Object transform(DataRecord record);
    
    /**
     * Returns the name of this transformer.
     * @return Transformer name
     */
    String getName();
}
