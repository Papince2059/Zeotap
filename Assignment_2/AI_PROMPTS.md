# AI Prompts Used for Assignment 2

This document summarizes all the prompts and instructions used to develop the Distributed Data Fan-Out & Transformation Engine using AI tools.

## Design & Architecture Prompts

### 1. Initial Architecture Design
**Prompt:**
"Design a distributed fan-out engine that reads data from source files and distributes records to multiple specialized sink systems (REST API, gRPC, Message Queues, Cassandra-like databases) with the following requirements:
- Support for CSV, JSONL, and fixed-width file formats
- Stream-based processing to handle files up to 100GB without loading into memory
- Format-specific transformations for each sink type
- Rate limiting to prevent overwhelming downstream services (REST: 50/s, gRPC: 100/s, MQ: 500/s, DB: 1000/s)
- Backpressure handling with bounded queues
- Retry logic with exponential backoff (max 3 retries)
- Dead Letter Queue for failed records
- Concurrency using ExecutorService with multi-threading
- Real-time metrics collection and status reporting every 5 seconds"

**Result:**
Created comprehensive architecture with layered design: Ingestion → Transformation → Distribution → Observability

---

### 2. Concurrency Model Design
**Prompt:**
"Implement concurrent processing for the fan-out engine using:
- ThreadPoolExecutor (not ForkJoinPool) for I/O-bound workload
- CompletableFuture for async sink operations without blocking
- Configurable core/max thread pool sizes
- Exponential backoff retry logic with configurable delay
- Explain why this approach suits the use case better than alternatives"

**Result:**
Implemented FanOutOrchestrator with:
- ThreadPoolExecutor with core=10, max=50 threads
- CompletableFuture-based async sink dispatch
- Exponential backoff: `delay = 2^retryCount * 100ms`
- Per-record async processing eliminating blocking waits

---

### 3. Memory Efficiency Strategy
**Prompt:**
"Design a streaming architecture that processes 100GB files with -Xmx512m heap:
- Use Java Stream API for lazy evaluation
- Implement file readers that return Stream<DataRecord>
- Ensure no record buffering except in bounded queues
- Create strategy pattern for different file formats
- Test with sample files"

**Result:**
- All FileReaders return `Stream<DataRecord>` with lazy evaluation
- CsvFileReader, JsonlFileReader, FixedWidthFileReader use streaming
- 64KB buffered readers for I/O efficiency
- Successfully processes 20-record sample file with <512MB heap

---

### 4. Rate Limiting Algorithm
**Prompt:**
"Implement a token-bucket rate limiter with:
- Smooth rate limiting without bursts
- Per-sink configurable limits (REST: 50/s, gRPC: 100/s, etc.)
- Burst capacity allowance (minimum 10 tokens)
- Thread-safe implementation with minimal locking
- Non-blocking tryAcquire() and blocking acquire()
- Dynamic refill based on elapsed time"

**Result:**
Created RateLimiter class with:
- Token refill: `(timePassed * tokensPerSecond) / 1_000_000_000.0`
- Burst size: `max(tokensPerSecond, 10)`
- Synchronized refillTokens() for thread safety
- Exponential wait time calculation

---

### 5. Backpressure Pattern
**Prompt:**
"Implement backpressure handling for fast producers / slow consumers:
- Use BlockingQueue with bounded capacity
- Per-sink capacity configuration (REST: 500, DB: 5000)
- offer() and put() operations with timeout
- Prevent OOM when downstream services are slow
- Integrate with rate limiter"

**Result:**
BackpressureBuffer with:
- Bounded LinkedBlockingQueue
- 30-second timeout on put operations
- remainingCapacity() tracking
- Integration with sink-specific configurations

---

## Implementation Prompts

### 6. File Reader Factory
**Prompt:**
"Create a FileReaderFactory using the Factory pattern that:
- Detects file type from extension (.csv, .jsonl, .txt)
- Returns appropriate reader implementation
- Handles CSV headers automatically
- Supports JSON object per line format
- Provides fallback for fixed-width files
- Returns Stream<DataRecord> for memory efficiency"

**Result:**
FileReaderFactory with:
- List of pluggable FileReader implementations
- canHandle() method for type detection
- getReader(filePath) method with exception handling
- readRecords(filePath) returning Stream

---

### 7. Transformation Strategy Pattern
**Prompt:**
"Implement transformers for different sink formats:
- JsonTransformer: REST API (JSON objects)
- ProtobufTransformer: gRPC (byte arrays)
- XmlTransformer: Message Queues (XML strings)
- AvroTransformer: Wide-Column DBs (CQL maps)
- Create TransformerFactory with SinkType → Transformer mapping
- Enable adding 5th sink without core changes"

**Result:**
Created Transformer interface and 4 implementations:
- Each transforms DataRecord to sink-specific format
- TransformerFactory uses switch statement for mapping
- Extensible: Adding Elasticsearch sink requires 2 changes (new transformer + factory registration)

---

### 8. Mock Sink Implementations
**Prompt:**
"Create mock sink implementations that simulate:
1. REST API (HTTP/2): 50 req/sec rate limit, 5-20ms latency simulation
2. gRPC: 100 req/sec, 2-10ms latency (bidirectional streaming simulation)
3. Message Queue (Kafka/RabbitMQ): 500 msg/sec, 1-5ms latency
4. Wide-Column DB (Cassandra): 1000 req/sec, 1-10ms latency
- Use CompletableFuture for async operations
- Simulate realistic failures for testing retry logic
- Track request/message/UPSERT counts"

**Result:**
4 sink classes extending BaseSink:
- RestApiSink, GrpcSink, MessageQueueSink, WideColumnDbSink
- Each with realistic latency simulation using Thread.sleep()
- CompletableFuture.supplyAsync() for non-blocking operations
- AtomicLong counters for operation tracking

---

### 9. Orchestrator Implementation
**Prompt:**
"Create FanOutOrchestrator that:
- Reads records using FileReaderFactory
- For each record, creates SinkEvents for all enabled sinks
- Applies transformations using TransformerFactory
- Dispatches to sinks asynchronously with rate limiting
- Handles errors with retry logic and DLQ
- Reports metrics every 5 seconds
- Manages lifecycle (startup, runtime, shutdown)
- Ensures zero data loss"

**Result:**
FanOutOrchestrator with:
- processRecord() creating per-sink SinkEvents
- dispatchToSink() with rate limiter acquire()
- handleError() with exponential backoff retry
- DeadLetterQueue for failed records (3+ retries)
- startMetricsThread() for periodic status reporting
- cleanup() for graceful shutdown

---

### 10. Configuration Management
**Prompt:**
"Create Spring Boot configuration using @ConfigurationProperties:
- YAML-based configuration (application.yaml)
- Input file path, format type, batch size
- Per-sink: endpoint, rate limit, buffer size, max retries, enabled flag
- Thread pool: core size, max size, queue capacity, keepalive
- Metrics interval (seconds)
- Allow override via command-line arguments"

**Result:**
FanOutConfig class with nested:
- InputConfig: filePath, fileType, batchSize
- SinkConfig per sink with all parameters
- ThreadPoolConfig: sizing and timeout
- metricsIntervalSeconds setting

---

### 11. Testing Strategy
**Prompt:**
"Create unit tests for:
- Transformers: Validate output format and structure
- RateLimiter: Token bucket behavior, blocking, tryAcquire
- BackpressureBuffer: Queue capacity, blocking behavior
- TransformerFactory: Correct transformer selection
- All tests should use JUnit 5 and Mockito where needed"

**Result:**
6 test classes with comprehensive coverage:
- JsonTransformerTest, ProtobufTransformerTest, AvroTransformerTest
- RateLimiterTest: tryAcquire, waiting behavior
- BackpressureBufferTest: capacity, blocking, isEmpty
- TransformerFactoryTest: all sink types

---

## Documentation Prompts

### 12. README and Architecture Documentation
**Prompt:**
"Write comprehensive README with:
- Project overview and key features
- Setup instructions and prerequisites
- Architecture diagram (text or visual description)
- Component breakdown with responsibilities
- Design decisions with rationale
- Configuration guide with YAML examples
- Running instructions with expected output
- Performance metrics and testing
- Assumptions about data and infrastructure
- List of all AI prompts used"

**Result:**
Comprehensive README.md with:
- 400+ lines
- Architecture diagram (ASCII)
- Component descriptions
- Design decisions section (8 major decisions with detailed explanations)
- Complete configuration reference
- Running examples with output samples
- Test instructions

---

## Key Technical Decisions Made

### Decision 1: Streaming vs Batch Processing
**Prompt:** "Why use Stream<DataRecord> instead of loading entire files into List?"
**Answer:** Stream provides lazy evaluation - one record at a time, constant memory regardless of file size. List would load entire 100GB file into heap.

### Decision 2: ThreadPoolExecutor vs ForkJoinPool
**Prompt:** "Which thread pool is better for I/O-bound fan-out operations?"
**Answer:** ThreadPoolExecutor for I/O-bound, ForkJoinPool for CPU-bound fork-join parallelism. Fan-out is I/O-bound (waiting for sinks).

### Decision 3: Token-Bucket vs Sliding Window Rate Limiting
**Prompt:** "Compare rate limiting algorithms for smoothness."
**Answer:** Token-bucket allows burst capacity and smooth refill; sliding window would be jittery. Choose token-bucket for downstream service stability.

### Decision 4: CompletableFuture vs Callbacks vs Reactive Streams
**Prompt:** "How to handle async sink operations?"
**Answer:** CompletableFuture for composition and error handling; Reactive Streams heavier than needed; callbacks error-prone.

### Decision 5: Dead Letter Queue Design
**Prompt:** "How to ensure zero data loss for unrecoverable failures?"
**Answer:** Persistent DLQ file with pipe-delimited format enables post-mortem analysis and replay/recovery mechanisms.

---

## Iteration History

### Iteration 1: Initial Development
- Created core models, file readers, transformers, sinks
- Implemented basic orchestrator with thread pool
- Issue: Lombok @Builder not working with final fields in Metrics

### Iteration 2: Metrics Fix
- Changed Metrics from @Data @Builder to @Data with custom constructor
- All AtomicLong fields now properly initialized
- Tests pass, metrics collection working

### Iteration 3: Compilation Fixes
- Fixed FileReader imports (BufferedReader vs FileReader namespace conflict)
- Fixed DeadLetterQueue try-with-resources for BufferedWriter
- Removed duplicate class closures from JsonlFileReader

### Iteration 4: Testing & Validation
- All 6 unit test classes pass
- Application successfully processes 20-record sample CSV
- Metrics show 80 successful dispatches (20 records × 4 sinks)
- Zero failures, proper throughput calculation

---

## Future Enhancements Prompts

### Prompt 1: Real Sink Integration
"Create actual HTTP client sink using OkHttp or HttpClient, gRPC stubs, Kafka producer, Cassandra driver"

### Prompt 2: Kubernetes Deployment
"Create Dockerfile and Kubernetes manifests for horizontal scaling with shared DLQ"

### Prompt 3: Observability
"Integrate Prometheus metrics export, Jaeger distributed tracing, custom health checks"

### Prompt 4: Data Validation
"Add JSON Schema validation, field-level transformations, data enrichment pipeline"

---

## Summary Statistics

- **Total Prompts Used**: 12 major + 5 decision discussions
- **Implementation Duration**: ~4 hours
- **Total Lines of Code**: 3,000+
- **Files Created**: 32 Java files
- **Test Coverage**: 6 test classes with 20+ test methods
- **Configuration Files**: 1 YAML + 1 POM.xml
- **Documentation**: 400+ line README

---

## How to Use These Prompts

1. **For Understanding**: Read prompts in order to understand architectural thinking
2. **For Learning**: Use prompts to learn design patterns and Java best practices
3. **For Extension**: Use prompts as templates when adding new features (e.g., 5th sink)
4. **For Reuse**: Copy prompts into your AI assistant to recreate or extend components

---

**Generated**: February 16, 2026
**Assignment**: Distributed Data Fan-Out & Transformation Engine
**Language**: Java 17
**Framework**: Spring Boot 4.0.2
