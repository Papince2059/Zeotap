package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.SinkType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for obtaining the correct transformer for a given sink type.
 * Uses strategy pattern to manage different transformation implementations.
 */
@Component
public class TransformerFactory {
    
    private final JsonTransformer jsonTransformer;
    private final XmlTransformer xmlTransformer;
    private final ProtobufTransformer protobufTransformer;
    private final AvroTransformer avroTransformer;
    
    @Autowired
    public TransformerFactory(JsonTransformer jsonTransformer,
                            XmlTransformer xmlTransformer,
                            ProtobufTransformer protobufTransformer,
                            AvroTransformer avroTransformer) {
        this.jsonTransformer = jsonTransformer;
        this.xmlTransformer = xmlTransformer;
        this.protobufTransformer = protobufTransformer;
        this.avroTransformer = avroTransformer;
    }
    
    /**
     * Gets the appropriate transformer for a sink type.
     * @param sinkType The type of sink
     * @return The transformer for that sink type
     */
    public Transformer getTransformer(SinkType sinkType) {
        return switch (sinkType) {
            case REST_API -> jsonTransformer;
            case MESSAGE_QUEUE -> xmlTransformer;
            case GRPC -> protobufTransformer;
            case WIDE_COLUMN_DB -> avroTransformer;
        };
    }
}
