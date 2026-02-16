package com.example.Assignment_2.model;

/**
 * Enumeration of supported sink types.
 */
public enum SinkType {
    REST_API("rest-api", "REST API"),
    GRPC("grpc", "gRPC"),
    MESSAGE_QUEUE("message-queue", "Message Queue"),
    WIDE_COLUMN_DB("wide-column-db", "Wide-Column DB");
    
    private final String key;
    private final String displayName;
    
    SinkType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static SinkType fromKey(String key) {
        for (SinkType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sink type: " + key);
    }
}
