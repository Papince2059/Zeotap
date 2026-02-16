# Distributed Data Fan-Out & Transformation Engine

A high-throughput, fault-tolerant Java application that reads records from source files and distributes them to multiple specialized systems with format transformation, rate limiting, and comprehensive error handling.

## Table of Contents
## Running the Application (Windows-first)

### Prerequisites
```powershell
# Check Java version (must be 17+)
java -version

# If you don't have Maven installed, use the included wrapper `mvnw.cmd`.
. 
```

### Build (Windows PowerShell)
```powershell
# From project root
.\mvnw.cmd clean package -DskipTests

# OR: run full build (includes tests)
.\mvnw.cmd clean install
```

### Run (PowerShell) — short & safe commands

Basic run (uses CSV by default):
```powershell
# Run in foreground (blocks terminal)
java -Xmx512m -jar .\target\Assignment_2-0.0.1-SNAPSHOT.jar
```

Run with a different input file:
```powershell
java -Xmx512m -jar .\target\Assignment_2-0.0.1-SNAPSHOT.jar --fanout.input.filePath=.\data\input.jsonl
```

Run with custom thread-pool settings:
```powershell
java -Xmx512m -jar .\target\Assignment_2-0.0.1-SNAPSHOT.jar --fanout.threadPool.corePoolSize=8 --fanout.threadPool.maxPoolSize=32
```

Run with increased logging:
```powershell
java -Xmx512m -jar .\target\Assignment_2-0.0.1-SNAPSHOT.jar --logging.level.com.example.Assignment_2=DEBUG
```

### Run in background and capture logs (PowerShell)
```powershell
# Create logs directory if needed
New-Item -ItemType Directory -Force -Path .\logs

# Start detached and redirect stdout/stderr
$proc = Start-Process -FilePath "java" -ArgumentList '-Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar' -RedirectStandardOutput .\logs\app.out.log -RedirectStandardError .\logs\app.err.log -NoNewWindow -PassThru

# $proc.Id contains the PID; to stop later: Stop-Process -Id $proc.Id -Force
```

### Run in background (cmd.exe)
```cmd
REM Run detached and append stdout/stderr to logs
mkdir logs 2>nul
start /B java -Xmx512m -jar target\Assignment_2-0.0.1-SNAPSHOT.jar > logs\app.out.log 2>&1
```

### After completion — cleanup & avoid locked JAR issues

When the application finishes, ensure there are no lingering `java.exe` processes holding `target\Assignment_2-0.0.1-SNAPSHOT.jar` before rebuilding:

Find processes that launched the JAR (PowerShell):
```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'Assignment_2-0.0.1-SNAPSHOT.jar' } | Select-Object ProcessId,CommandLine
```

Stop them (PowerShell):
```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'Assignment_2-0.0.1-SNAPSHOT.jar' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

Or stop by PID directly if you have the PID:
```powershell
Stop-Process -Id <PID> -Force
```

If you prefer cmd.exe:
```cmd
REM Find java PIDs in tasklist and kill by PID

**Rationale**:
```

Once processes are stopped, rebuild safely:
```powershell
.\mvnw.cmd clean package -DskipTests
```

### Follow logs in PowerShell
```powershell
Get-Content .\logs\app.out.log -Wait -Tail 200
```

### Expected output & metrics
When the app runs you will see periodic status reports similar to:

```
=== Fan-Out Engine Status ===
Elapsed Time: 5 seconds
Total Processed: 20
Succeeded: 80
Failed: 0
Throughput: 4.00 records/sec

Success/Failure by Sink:
  REST API: 20 success, 0 failure
  gRPC: 20 success, 0 failure
  Message Queue: 20 success, 0 failure
  Wide-Column DB: 20 success, 0 failure
=============================
```

### Troubleshooting (Windows)

| Issue | Solution |
|-------|----------|
| `java: command not found` | Install Java 17+ or add to PATH |
| `mvn: command not found` | Use `mvnw.cmd` in project root or install Maven |
| `Failed to clean project: Failed to delete ... .jar` | Stop running java.exe processes that reference the JAR (see "After completion" commands) |
| `FileNotFoundException` for input file | Ensure `.\data\input.csv` exists or update `--fanout.input.filePath` |
| `OutOfMemoryError` | Increase heap: `java -Xmx1024m -jar ...` |
| Slow performance | Increase thread pool: `--fanout.threadPool.maxPoolSize=64` |
- Prevents fast producer from overwhelming slow consumer (DSL protection)
- Bounded capacity forces producer to wait when buffer full
- No risk of OOM from queue overflow
- Gives feedback to ingestion layer when sinks can't keep up

**Configuration**:
```yaml
sinks:
  rest-api:
    bufferSize: 500    # REST API is slower
  wide-column-db:
    bufferSize: 5000   # DB can handle more
```

### 4. **Retry Logic with Exponential Backoff**

**Decision**: Max 3 retries per record with exponential backoff

**Rationale**:
- Handles transient failures (network glitches, momentary unavailability)
- Exponential backoff (2^n * 100ms) prevents thundering herd
- Hard limit prevents infinite retry loops
- Failed records go to DLQ for later analysis

**Logic**:
```java
private void handleError(SinkEvent event, SinkType sinkType, ...) {
    event.incrementRetry(exception.getMessage());
    
    if (event.canRetry()) {  // Max 3 retries
        long delayMs = (long) Math.pow(2, event.getRetryCount()) * 100;
        // Retry with delay
    } else {
        deadLetterQueue.add(event);  // Unrecoverable
    }
}
```

### 5. **Executor Service with Configurable Thread Pool**

**Decision**: Use `ThreadPoolExecutor` (not ForkJoinPool) with LinkedBlockingQueue

**Rationale**:
- Per-record dispatch is optimal for I/O-bound workload
- ForkJoinPool is better for CPU-bound fork-join tasks
- Can scale core threads from 10 to 50 based on CPU cores
- `CallerRunsPolicy` provides backpressure at thread pool level

**Configuration**:
```yaml
threadPool:
  corePoolSize: 10        # Base threads
  maxPoolSize: 50         # Max threads (should match CPU cores)
  queueCapacity: 1000     # Queue size
  keepAliveSeconds: 60    # Idle thread timeout
```

### 6. **Strategy Pattern for Transformations**

**Decision**: Factory + Strategy pattern for format conversion

**Rationale**:
- Each sink needs different format (JSON, Protobuf, XML, Avro)
- Strategy pattern encapsulates transformation logic
- Can add 5th sink (e.g., Elasticsearch) without modifying orchestrator
- Makes testing individual transformers easy

**Extension Example** (to add 5th sink):
```java
// 1. Create new transformer
@Component
public class ElasticsearchTransformer implements Transformer { ... }

// 2. Register in factory
public Transformer getTransformer(SinkType sinkType) {
    return switch(sinkType) {
        case ELASTICSEARCH -> elasticsearchTransformer;
        // ... existing cases
    };
}
// Done! No orchestrator changes needed
```

### 7. **CompletableFuture for Async Operations**

**Decision**: Use `CompletableFuture` instead of callbacks or blocking waits

**Rationale**:
- Non-blocking async processing at sink level
- Can compose multiple async operations easily
- Functional error handling with `whenComplete()`
- Better than threads for network I/O

**Pattern**:
```java
sink.send(event)
    .whenComplete((result, exception) -> {
        if (exception != null) {
            handleError(...);
        } else {
            handleSuccess(result);
        }
    });
```

### 8. **Dead Letter Queue for Zero Data Loss**

**Decision**: Persistent DLQ file for failed records

**Rationale**:
- Guarantees zero data loss
- Failed records can be analyzed for root causes
- Enables replay/recovery mechanism
- Audit trail for compliance

**Format**: Pipe-delimited for easy parsing
```
event_id|sink_type|record_id|retry_count|last_error
```

## Assumptions

### 1. **File Format Assumptions**
- CSV files have headers in first row
- JSONL files have one complete JSON object per line
- Fixed-width files use consistent column positions
- Files are UTF-8 encoded

### 2. **Network Assumptions**
- Simulated sinks have realistic latency (5-20ms REST, 1-10ms DB)
- Networks are usually available (transient failures, not permanent)
- Maximum 3 retries is sufficient for transient failures
- Exponential backoff prevents cascading failures

### 3. **Data Assumptions**
- Records are less than 1MB each (streaming assumes reasonable record size)
- No duplicate IDs within a single run (but can have sequential runs)
- Source data doesn't change during ingestion

### 4. **Infrastructure Assumptions**
- System has at least 2 CPU cores (thread pool starts at 10 threads)
- Disk I/O is not bottleneck (file reading is fast)
- Java heap >= 512MB for operation (can process 100GB file)
- DLQ file system has sufficient free space

### 5. **Operational Assumptions**
- Application runs until all records processed, then exits
- Metrics logging doesn't impact performance significantly
- Status prints every 5 seconds are acceptable overhead
- Spring Boot auto-configuration is suitable for this use case

## Configuration

All configuration is in `src/main/resources/application.yaml`:

### Input Configuration
```yaml
fanout:
  input:
    filePath: "./data/input.csv"     # Path to source file
    fileType: "csv"                  # Format: csv, jsonl, fixed-width
    batchSize: 100                   # Records per batch (not used currently)
```

### Per-Sink Configuration
```yaml
  sinks:
    rest-api:
      sinkType: REST_API             # Sink identifier
      endpoint: "http://..."         # Target endpoint
      rateLimit: 50                  # Requests per second
      bufferSize: 500                # Backpressure queue size
      maxRetries: 3                  # Max retry attempts
      enabled: true                  # Enable/disable sink
```

### Thread Pool Configuration
```yaml
  threadPool:
    corePoolSize: 10                 # Minimum threads
    maxPoolSize: 50                  # Maximum threads
    queueCapacity: 1000              # Thread pool queue size
    keepAliveSeconds: 60             # Idle thread timeout
```

### Metrics Configuration
```yaml
  metricsIntervalSeconds: 5          # Status print interval
```

## Running the Application

### Prerequisites
```powershell
# Check Java version (must be 17+)
java -version

# Maven wrapper is included (no need to install Maven)
# Just ensure Java is installed and working
```

### Step-by-Step Build and Run

#### Step 1: Clone and Navigate to Project
```bash
# Navigate to project root directory
cd Assignment_2
```

#### Step 2: Build the Project
```powershell
# Clean and build without tests (faster) — RECOMMENDED for Windows
.\mvnw.cmd clean package -DskipTests

# OR: Clean and build with all tests
.\mvnw.cmd clean install
```

#### Step 3: Run the Application

**Option A: Basic Run (Uses CSV by default)**
```bash
java -jar target/Assignment_2-0.0.1-SNAPSHOT.jar
```

**Option B: Run with Maximum Heap Memory (for large files)**
```bash
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar
```

**Option C: Run with Different Input File (JSONL)**
```bash
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar \
  --fanout.input.filePath=./data/input.jsonl
```

**Option D: Run with Custom Thread Pool Settings**
```bash
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar \
  --fanout.threadPool.corePoolSize=8 \
  --fanout.threadPool.maxPoolSize=32
```

**Option E: Run with Custom Rate Limits (faster testing)**
```bash
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar \
  --fanout.sinks.rest-api.rateLimit=500 \
  --fanout.sinks.grpc.rateLimit=1000
```

**Option F: Run with Increased Logging**
```bash
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar \
  --logging.level.com.example.Assignment_2=DEBUG
```

**Option G: Run with All Customizations (Production-like)**
```bash
java -Xmx1024m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar \
  --fanout.input.filePath=./data/input.csv \
  --fanout.threadPool.corePoolSize=16 \
  --fanout.threadPool.maxPoolSize=64 \
  --fanout.metricsIntervalSeconds=10
```

#### Step 4: Run Tests (Optional)

**Windows PowerShell:**
```powershell
# Run all tests
.\mvnw.cmd test

# Run specific test class
.\mvnw.cmd test -Dtest=RateLimiterTest

# Run tests with coverage
.\mvnw.cmd test jacoco:report
```

**Linux/Mac:**
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=RateLimiterTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Quick Start Commands (Windows PowerShell)

```powershell
# Navigate to project
cd "C:\Users\papin\Downloads\Assignment_2\Assignment_2"

# Build and run in one command
.\mvnw.cmd clean package -DskipTests; java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar

# Or build only
.\mvnw.cmd clean package -DskipTests

# Then run
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar
```

### Quick Start Commands (Linux/Mac)

```bash
# Navigate to project
cd Assignment_2

# Build and run in one command
./mvnw clean package -DskipTests && java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar

# Or build only
./mvnw clean package -DskipTests

# Then run
java -Xmx512m -jar target/Assignment_2-0.0.1-SNAPSHOT.jar
```

### Expected Output

When you run the application, you should see output like:

```
15:41:47.183 Starting Assignment 2 - Distributed Data Fan-Out & Transformation Engine
15:41:50.101 Input file: ./data/input.csv
15:41:50.105 Using reader: CsvFileReader for file: ./data/input.csv
15:41:50.215 Initialized sink: REST API with rate limit: 50 req/sec buffer size: 500
15:41:50.216 Initialized sink: gRPC with rate limit: 100 req/sec buffer size: 1000
15:41:50.217 Initialized sink: Message Queue with rate limit: 500 req/sec buffer size: 2000
15:41:50.218 Initialized sink: Wide-Column DB with rate limit: 1000 req/sec buffer size: 5000
15:41:50.221 Created thread pool: core=10, max=50, queue=1000
15:41:50.225 Starting Fan-Out Orchestrator
15:41:50.228 Processing file: ./data/input.csv (20 total records)

[Processing in progress... records being distributed to all 4 sinks...]

=== Fan-Out Engine Status ===
Elapsed Time: 5 seconds
Total Processed: 20
Succeeded: 80
Failed: 0
Throughput: 4.00 records/sec

Success/Failure by Sink:
  REST API: 20 success, 0 failure
  gRPC: 20 success, 0 failure
  Message Queue: 20 success, 0 failure
  Wide-Column DB: 20 success, 0 failure
=============================

[More status updates every 5 seconds...]

=== Fan-Out Engine Status ===
Elapsed Time: 10 seconds
Total Processed: 20
Succeeded: 80
Failed: 0
Throughput: 2.00 records/sec

Success/Failure by Sink:
  REST API: 20 success, 0 failure
  gRPC: 20 success, 0 failure
  Message Queue: 20 success, 0 failure
  Wide-Column DB: 20 success, 0 failure
=============================

15:43:57.456 Assignment 2 completed successfully
```

### Understanding the Output

- **Elapsed Time**: How long the application has been running
- **Total Processed**: Number of input records read
- **Succeeded**: Number of successful sink dispatches (records × sinks)
- **Failed**: Number of failed dispatches (should be 0 with sample data)
- **Throughput**: Records processed per second
- **Success/Failure by Sink**: Breakdown of success rate per sink type

### Performance Metrics

With 20 records and 4 sinks per record = 80 total dispatches:

- **Expected Runtime**: ~10-50 seconds (depends on simulated latency)
- **Throughput**: 0.4-4 records/sec (with simulated sink latency)
- **Memory Usage**: <512MB even with 100GB source file
- **CPU Usage**: Scales with thread pool size and concurrent sinks
- **Success Rate**: 100% for sample data (no errors)

### Troubleshooting

| Issue | Solution |
|-------|----------|
| `java: command not found` | Install Java 17+ or add to PATH |
| `mvn: command not found` | Use `.\mvnw.cmd` (Windows) or `./mvnw` (Linux/Mac) - Maven wrapper is included in project |
| `FileNotFoundException` for input file | Ensure `./data/input.csv` exists or update `--fanout.input.filePath` |
| `OutOfMemoryError` | Increase heap: `java -Xmx1024m -jar ...` |
| Tests fail | Run `mvn clean install` to rebuild |
| Slow performance | Increase thread pool: `--fanout.threadPool.maxPoolSize=64` |

## Testing

### Run All Tests

```powershell
# Windows
.\mvnw.cmd test
```

```bash
# Linux/Mac
./mvnw test
```

### Test Coverage

#### Unit Tests
1. **Transformers** (`transformation/`)
   - `JsonTransformerTest`: Maps to JSON structure
   - `ProtobufTransformerTest`: Serializes to bytes
   - `AvroTransformerTest`: Creates CQL-compatible maps
   - `TransformerFactoryTest`: Factory pattern validation

2. **Throttling** (`throttling/`)
   - `RateLimiterTest`: Token-bucket algorithm validation
   - `BackpressureBufferTest`: Queue capacity and blocking behavior

#### Integration Tests (conceptual, Spring Boot Test layer)
- Full orchestrator with mocked sinks
- File reader factory with different formats
- E2E metrics collection

### Running Specific Tests

```powershell
# Windows PowerShell
# Run transformer tests
.\mvnw.cmd test -Dtest=*TransformerTest

# Run throttling tests
.\mvnw.cmd test -Dtest=*RateLimiter*,*BackpressureBuffer*

# Run with coverage report
.\mvnw.cmd test jacoco:report
```

```bash
# Linux/Mac
# Run transformer tests
./mvnw test -Dtest=*TransformerTest

# Run throttling tests  
./mvnw test -Dtest=*RateLimiter*,*BackpressureBuffer*

# Run with coverage report
./mvnw test jacoco:report
```

## AI Prompts Used

### Design Phase Prompts

1. **Architecture Design**
   - "Design a distributed fan-out engine with multiple sink types, rate limiting, and backpressure handling"
   - "Implement strategy pattern for data transformations to different formats"
   - "Create a token-bucket rate limiter that prevents overwhelming downstream services"

2. **Concurrency Approach**
   - "Implement async processing using CompletableFuture for non-blocking sink operations"
   - "Design thread pool with configurable core/max size for handling concurrent record processing"
   - "Implement exponential backoff retry logic with maximum retry limit"

3. **Memory Optimization**
   - "Design streaming file reader that doesn't load entire file into memory"
   - "Support CSV, JSONL, and fixed-width file formats with stream-based processing"
   - "Implement bounded BlockingQueue for backpressure to prevent OOM"

### Implementation Phase Prompts

4. **Core Components**
   - "Create file reader factory with support for multiple formats using strategy pattern"
   - "Implement transformers for JSON, Protobuf, XML, and Avro/CQL formats"
   - "Build mock sinks that simulate REST API, gRPC, Message Queue, and Wide-Column DB"

5. **Orchestration**
   - "Create main orchestrator that coordinates file reading, transformation, and distribution"
   - "Implement metrics collection with per-sink success/failure tracking"
   - "Create dead letter queue for handling unrecoverable failures"

6. **Configuration & Bootstrap**
   - "Create Spring Boot configuration with YAML property binding for all settings"
   - "Implement application startup that reads from config and initializes all components"
   - "Create sample input files for testing with realistic data"

### Testing Phase Prompts

7. **Test Implementation**
   - "Write unit tests for all transformer implementations"
   - "Create tests for rate limiter and backpressure buffer components"
   - "Test transformer factory pattern with different sink types"

8. **Documentation**
   - "Write comprehensive README with setup, architecture, and design decisions"
   - "Create architecture diagram explaining data flow through all components"
   - "Document assumptions about file formats, network, and infrastructure"

## Project Statistics

- **Total Files**: 30+ Java files
- **Lines of Code**: ~3,000+ LOC
- **Test Coverage**: 6 unit test classes, 20+ test methods
- **Configuration Files**: 1 YAML, 1 POM
- **Sample Data**: CSV (20 records), JSONL (10 records)

## Future Enhancements

1. **Add Real Sink Implementations**
   - Actual HTTP client for REST API
   - gRPC generated stubs
   - Kafka producer
   - Cassandra driver

2. **Advanced Monitoring**
   - Metrics export (Prometheus)
   - Distributed tracing (Jaeger)
   - Custom health checks

3. **Scalability**
   - Kafka consumer for distributed processing
   - Kubernetes deployment manifests
   - Horizontal scaling with shared DLQ

4. **Data Validation**
   - JSON Schema validation
   - Custom field transformations
   - Data enrichment layer

## License

This is an assignment project. Use freely for educational purposes.

## Contact

For questions or issues, please refer to the assignment rubric and requirements document.
