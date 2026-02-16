package com.example.Assignment_2.observability;

import com.example.Assignment_2.model.Metrics;
import com.example.Assignment_2.model.ProcessingResult;
import com.example.Assignment_2.model.SinkType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observability layer for monitoring the fan-out engine.
 * Tracks metrics and provides status reporting.
 */
@Slf4j
@Component
public class MetricsCollector {
    
    private final Metrics metrics;
    
    public MetricsCollector() {
        this.metrics = new Metrics();
    }
    
    public void recordProcessed() {
        metrics.recordProcessed();
    }
    
    public void recordResult(ProcessingResult result) {
        if (result.isSuccess()) {
            metrics.recordSuccess(result.getSinkType());
        } else {
            metrics.recordFailure(result.getSinkType());
        }
    }
    
    public Metrics getMetrics() {
        return metrics;
    }
    
    /**
     * Prints a status report showing current metrics.
     */
    public void printStatus() {
        long processed = metrics.getRecordsProcessed().get();
        long succeeded = metrics.getRecordsSucceeded().get();
        long failed = metrics.getRecordsFailed().get();
        double throughput = metrics.getThroughput();
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Fan-Out Engine Status ===\n");
        sb.append("Elapsed Time: ").append(metrics.getElapsedSeconds()).append(" seconds\n");
        sb.append("Total Processed: ").append(processed).append("\n");
        sb.append("Succeeded: ").append(succeeded).append("\n");
        sb.append("Failed: ").append(failed).append("\n");
        sb.append(String.format("Throughput: %.2f records/sec\n", throughput));
        sb.append("\nSuccess/Failure by Sink:\n");
        
        for (SinkType sinkType : SinkType.values()) {
            long success = metrics.getSuccessBySink()
                    .getOrDefault(sinkType, new AtomicLong(0)).get();
            long failure = metrics.getFailureBySink()
                    .getOrDefault(sinkType, new AtomicLong(0)).get();
            sb.append(String.format("  %s: %d success, %d failure\n", 
                    sinkType.getDisplayName(), success, failure));
        }
        sb.append("=============================\n");
        
        log.info(sb.toString());
    }
}
