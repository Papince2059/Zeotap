package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.DataRecord;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms data records to XML format.
 * Used for legacy message queue sinks that require XML payloads.
 */
@Slf4j
@Component
public class XmlTransformer implements Transformer {
    
    private final XmlMapper xmlMapper = new XmlMapper();
    
    @Override
    public Object transform(DataRecord record) {
        Map<String, Object> output = new HashMap<>();
        output.put("id", record.getId());
        output.put("sequenceNumber", record.getSequenceNumber());
        output.put("data", convertMapToXmlNodes(record.getFields()));
        output.put("timestamp", System.currentTimeMillis());
        
        try {
            return xmlMapper.writeValueAsString(output);
        } catch (Exception e) {
            log.error("Error transforming record to XML", e);
            throw new RuntimeException("XML transformation failed", e);
        }
    }
    
    @Override
    public String getName() {
        return "XML Transformer";
    }
    
    private Map<String, String> convertMapToXmlNodes(Map<String, Object> fields) {
        Map<String, String> xmlFields = new HashMap<>();
        fields.forEach((key, value) -> {
            xmlFields.put(key, value != null ? value.toString() : "");
        });
        return xmlFields;
    }
}
