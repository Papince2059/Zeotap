package com.example.Assignment_2.transformation;

import com.example.Assignment_2.model.SinkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransformerFactoryTest {
    
    private TransformerFactory factory;
    
    @BeforeEach
    public void setUp() {
        factory = new TransformerFactory(
                new JsonTransformer(),
                new XmlTransformer(),
                new ProtobufTransformer(),
                new AvroTransformer()
        );
    }
    
    @Test
    public void testGetJsonTransformer() {
        Transformer transformer = factory.getTransformer(SinkType.REST_API);
        assertNotNull(transformer);
        assertTrue(transformer instanceof JsonTransformer);
    }
    
    @Test
    public void testGetProtobufTransformer() {
        Transformer transformer = factory.getTransformer(SinkType.GRPC);
        assertNotNull(transformer);
        assertTrue(transformer instanceof ProtobufTransformer);
    }
    
    @Test
    public void testGetXmlTransformer() {
        Transformer transformer = factory.getTransformer(SinkType.MESSAGE_QUEUE);
        assertNotNull(transformer);
        assertTrue(transformer instanceof XmlTransformer);
    }
    
    @Test
    public void testGetAvroTransformer() {
        Transformer transformer = factory.getTransformer(SinkType.WIDE_COLUMN_DB);
        assertNotNull(transformer);
        assertTrue(transformer instanceof AvroTransformer);
    }
}
