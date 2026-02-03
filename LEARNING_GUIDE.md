# Claims Integration Hub - Complete Learning Guide

## Overview

This project is a **Spring Integration learning application** that demonstrates Enterprise Integration Patterns (EIP) with Azure messaging. It simulates an insurance claims processing system.

**Location**: `D:/kaizen_ws/claims-integration-hub/`

**Domain**: Insurance claims processing (not related to any specific job - a neutral domain for learning)

**Purpose**: Learn and demonstrate Spring Integration patterns for interview preparation

---

## What Problem Does This Solve?

In enterprise systems, you often need to:
- Accept data from multiple sources (JSON API, XML feeds, CSV batch files)
- Validate and transform data
- Route to different processors based on content
- Process in batches and aggregate results
- Send to external systems (payment processing, audit logs)
- Handle failures gracefully

**Without Spring Integration**: You write tangled code mixing HTTP handling, validation, routing logic, and external calls.

**With Spring Integration**: You compose small, reusable patterns into clear flows.

---

## The Three Phases

| Phase | Focus | Patterns Covered |
|-------|-------|------------------|
| **Phase 1** | Core message flow | Gateway, Channel, Transformer, Filter, Router, Service Activator |
| **Phase 2** | Batch processing | Splitter, Aggregator, Correlation, Release Strategy |
| **Phase 3** | Azure messaging | Event Hubs, Service Bus, Dead Letter Queue |

---

# Phase 1: Core Spring Integration Flow

## What Was Built

A REST API that accepts insurance claims and processes them through a message-driven pipeline.

## The Flow

```
POST /api/claims (JSON)
       │
       ▼
  ┌─────────┐
  │ GATEWAY │ ──── Converts method call to message
  └────┬────┘
       │
       ▼
  ┌─────────────┐
  │ TRANSFORMER │ ──── Enriches with IDs, timestamps
  └──────┬──────┘
         │
         ▼
    ┌────────┐
    │ FILTER │ ──── Validates required fields
    └────┬───┘
         │
    ┌────┴────┐
    ▼         ▼
  Valid    Invalid ───► Logged for review
    │
    ▼
  ┌────────┐
  │ ROUTER │ ──── Routes by claim type
  └────┬───┘
       │
  ┌────┴────────────┬────────────┬──────────────┐
  ▼                 ▼            ▼              ▼
AUTO            PROPERTY      HEALTH       HIGH-VALUE
Handler         Handler       Handler       Handler
  │                 │            │              │
  └─────────────────┴────────────┴──────────────┘
                          │
                          ▼
                   Processed Claims
```

## Patterns Explained

### 1. Gateway (`ClaimGateway.java`)

**What it does**: Provides a clean Java interface that hides all messaging complexity.

**Why it matters**: Your business code calls `gateway.submitClaim(claim)` - it doesn't know about channels, messages, or Spring Integration.

```java
@MessagingGateway
public interface ClaimGateway {
    @Gateway(requestChannel = "claimInputChannel")
    Claim submitClaim(Claim claim, @Header("source") String source);
}
```

**Interview answer**: "Gateway converts method calls to messages and vice versa. It lets me keep my service layer clean - they just call a method, and Spring Integration handles the messaging infrastructure."

---

### 2. Channel (`ClaimChannels.java`)

**What it does**: Pipes that connect components. Producers write to channels, consumers read from them.

**Why it matters**: Decouples components. The producer doesn't know who consumes. You can swap implementations without changing code.

```java
@Bean
public MessageChannel claimInputChannel() {
    return new DirectChannel();  // Synchronous, single consumer
}

@Bean
public MessageChannel paymentChannel() {
    return new QueueChannel(100);  // Async, buffers messages
}
```

**Types**:
- `DirectChannel`: Synchronous, one consumer, fails fast
- `QueueChannel`: Asynchronous, buffers messages, handles backpressure
- `PublishSubscribeChannel`: Multiple consumers get same message

**Interview answer**: "Channels decouple producers from consumers. I use DirectChannel for synchronous flows where I need immediate feedback, and QueueChannel when I need to buffer messages or handle backpressure."

---

### 3. Transformer (`ClaimTransformer.java`)

**What it does**: Converts message payload from one format to another.

**Why it matters**: External systems send different formats (JSON, XML, CSV). Transformer normalizes to internal domain model.

```java
@Transformer(inputChannel = "claimInputChannel", outputChannel = "validatedClaimsChannel")
public Claim transformAndEnrich(Claim claim, @Header("source") String source) {
    // Add IDs, timestamps, flag high-value claims
    claim.setClaimId(UUID.randomUUID().toString());
    claim.setReceivedAt(Instant.now());
    claim.setFlaggedForReview(claim.isHighValue());
    return claim;
}
```

**Interview answer**: "Transformer modifies the message payload. I use it to normalize different input formats to our internal domain model, and to enrich messages with metadata like timestamps and correlation IDs."

---

### 4. Filter (`ClaimValidationFilter.java`)

**What it does**: Makes a YES/NO decision. Pass the message or reject it.

**Why it matters**: Validates data before processing. Invalid claims go to error handling, not the main flow.

```java
@Filter(
    inputChannel = "validatedClaimsChannel",
    outputChannel = "routingChannel",
    discardChannel = "invalidClaimsChannel"  // Don't lose rejects!
)
public boolean isValidClaim(Claim claim) {
    if (claim.getPolicyNumber() == null) return false;
    if (claim.getAmount().compareTo(BigDecimal.ZERO) <= 0) return false;
    return true;
}
```

**Key concept**: Always configure `discardChannel` - you don't want to silently lose messages.

**Interview answer**: "Filter is for binary decisions - should this message continue or not? I always configure a discardChannel so rejected messages aren't lost. They go to error handling where ops can investigate."

---

### 5. Router (`ClaimTypeRouter.java`)

**What it does**: Sends messages to different channels based on content.

**Why it matters**: Different claim types need different processing logic. Router directs traffic.

```java
@Router(inputChannel = "routingChannel")
public String routeByClaimType(Claim claim) {
    if (claim.isHighValue()) {
        return "highValueClaimsChannel";  // Fraud review
    }
    return switch (claim.getClaimType()) {
        case AUTO -> "autoClaimsChannel";
        case PROPERTY -> "propertyClaimsChannel";
        case HEALTH -> "healthClaimsChannel";
        default -> "generalClaimsChannel";
    };
}
```

**Filter vs Router**:
- **Filter**: YES/NO (binary)
- **Router**: WHICH ONE? (multiple destinations)

**Interview answer**: "Router is for content-based routing - directing messages to different channels based on their content. I used it to route claims by type so each type can have specialized processing logic."

---

### 6. Service Activator (`AutoClaimHandler.java`, etc.)

**What it does**: Invokes business logic when a message arrives.

**Why it matters**: This is where your actual work happens. The other patterns get the message here; Service Activator does the processing.

```java
@ServiceActivator(inputChannel = "autoClaimsChannel", outputChannel = "processedClaimsChannel")
public Claim processAutoClaim(Claim claim) {
    // Actual business logic
    log.info("Processing AUTO claim: {}", claim.getClaimId());

    // Auto-approve small claims
    if (claim.getAmount().compareTo(new BigDecimal("1000")) < 0) {
        claim.setStatus(ClaimStatus.APPROVED);
    } else {
        claim.setStatus(ClaimStatus.PENDING_REVIEW);
    }

    return claim;
}
```

**Interview answer**: "Service Activator is the bridge between messaging and business logic. The integration patterns route the message to the right place; the Service Activator does the actual work - calling services, updating databases, making decisions."

---

## Phase 1 Files

| File | Pattern | Purpose |
|------|---------|---------|
| `ClaimGateway.java` | Gateway | Entry point, hides messaging |
| `ClaimChannels.java` | Channel | Defines all message pipes |
| `ClaimTransformer.java` | Transformer | Enriches claims with metadata |
| `ClaimValidationFilter.java` | Filter | Validates or rejects claims |
| `ClaimTypeRouter.java` | Router | Routes by claim type |
| `AutoClaimHandler.java` | Service Activator | Processes auto claims |
| `PropertyClaimHandler.java` | Service Activator | Processes property claims |
| `HealthClaimHandler.java` | Service Activator | Processes health claims |
| `HighValueClaimHandler.java` | Service Activator | Fraud review for high-value |
| `InvalidClaimHandler.java` | Service Activator | Handles rejected claims |

---

# Phase 2: Splitter and Aggregator

## What Was Built

Batch processing capability - accept a CSV file with multiple claims, process each independently, and return a summary.

## The Flow

```
POST /api/claims/batch (CSV file)
       │
       │  "policyNumber,claimType,amount,..."
       │  "POL-001,AUTO,2500,..."
       │  "POL-002,HEALTH,500,..."
       │  "POL-003,PROPERTY,15000,..."
       │
       ▼
  ┌───────────┐
  │ SPLITTER  │ ──── Breaks CSV into individual claims
  └─────┬─────┘
        │
  ┌─────┴─────┬─────────┬─────────┐
  ▼           ▼         ▼         ▼
Claim 1    Claim 2   Claim 3   Claim N
  │           │         │         │
  ▼           ▼         ▼         ▼
(each processed independently through Phase 1 flow)
  │           │         │         │
  └───────────┴─────────┴─────────┘
              │
              ▼
       ┌────────────┐
       │ AGGREGATOR │ ──── Waits for all, combines into summary
       └─────┬──────┘
             │
             ▼
       BatchSummary {
         totalClaims: 10,
         validClaims: 8,
         invalidClaims: 2,
         totalAmount: $125,000,
         byType: {AUTO: 4, HEALTH: 3, PROPERTY: 3}
       }
```

## Patterns Explained

### 7. Splitter (`BatchClaimSplitter.java`)

**What it does**: Takes one message containing many items, produces many messages (one per item).

**Why it matters**: Batch files contain multiple records. Each needs independent processing.

```java
@Splitter(inputChannel = "batchInputChannel", outputChannel = "batchProcessingChannel")
public Collection<Claim> splitBatch(String csvContent, @Header("batchId") String batchId) {
    List<CsvClaimRecord> records = parseCsv(csvContent);

    return records.stream()
        .map(record -> convertToClaim(record, batchId))
        .collect(Collectors.toList());

    // Each claim in the returned list becomes a SEPARATE message
}
```

**Key concept**: Spring Integration automatically adds correlation headers:
- `correlationId`: Same for all messages from this split
- `sequenceNumber`: Position (1, 2, 3...)
- `sequenceSize`: Total count

**Interview answer**: "Splitter takes one message and produces many. Each item in the returned collection becomes a separate message that flows independently. Spring automatically adds correlation headers so an Aggregator can recombine them later."

---

### 8. Aggregator (`BatchResultAggregator.java`)

**What it does**: Collects related messages and combines them into one.

**Why it matters**: After splitting and processing, you need to report on the batch as a whole.

```java
// How to group messages together
@CorrelationStrategy
public String correlateByBatchId(Message<Claim> message) {
    return message.getPayload().getCorrelationId();  // Same batch = same group
}

// When to release the group
@ReleaseStrategy
public boolean canRelease(List<Message<Claim>> messages) {
    Integer expectedSize = messages.get(0).getHeaders().get("sequenceSize", Integer.class);
    return messages.size() >= expectedSize;  // All messages received
}

// What to produce
@Aggregator(inputChannel = "aggregationChannel", outputChannel = "batchSummaryChannel")
public BatchSummary aggregateBatch(List<Claim> claims) {
    return BatchSummary.builder()
        .totalClaims(claims.size())
        .totalAmount(claims.stream().map(Claim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
        .build();
}
```

**Three key concepts**:
1. **Correlation Strategy**: How to group messages (by batch ID)
2. **Release Strategy**: When to release (all received, or timeout)
3. **Output Processor**: What to produce (the summary)

**Interview answer**: "Aggregator combines multiple messages into one. It needs three things: a correlation strategy to group messages, a release strategy to know when the group is complete, and an output processor to produce the final result. The tricky part is handling timeouts - what if one message never arrives? I use groupTimeout to release partial results."

---

## Phase 2 Files

| File | Pattern | Purpose |
|------|---------|---------|
| `BatchClaimGateway.java` | Gateway | Entry point for batch processing |
| `CsvClaimRecord.java` | DTO | Represents one CSV row |
| `BatchSummary.java` | DTO | Aggregation result |
| `BatchClaimSplitter.java` | Splitter | Breaks CSV into claims |
| `BatchResultAggregator.java` | Aggregator | Combines results |
| `BatchProcessingFlow.java` | Flow | Wires batch processing |

---

# Phase 3: Azure Service Bus and Event Hubs

## What Was Built

Integration with Azure messaging services:
- **Event Hubs**: Audit trail (all events, multiple consumers)
- **Service Bus**: Payment processing (one consumer, exactly-once)

## The Flow

```
Processed Claims
       │
       ▼
┌──────────────────────────────────────┐
│      PUBLISH-SUBSCRIBE               │
│  (same message to multiple channels) │
└──────────────┬───────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
┌─────────────┐  ┌─────────────┐
│auditChannel │  │paymentChannel│
└──────┬──────┘  └──────┬──────┘
       │                │
       ▼                ▼
┌─────────────────┐  ┌─────────────────┐
│  EVENT HUBS     │  │  SERVICE BUS    │
│  "claim-events" │  │  "payment-queue"│
│                 │  │                 │
│ Multiple        │  │ One consumer    │
│ consumers:      │  │ processes each  │
│ • Analytics     │  │                 │
│ • Audit         │  │ Dead Letter     │
│ • Fraud ML      │  │ Queue for       │
│                 │  │ failures        │
└─────────────────┘  └─────────────────┘
```

## Concepts Explained

### Event Hubs (`EventHubProducer.java`)

**What it is**: High-throughput event streaming platform (like Kafka).

**Use cases**:
- Audit trail (compliance requirement)
- Analytics dashboard (real-time metrics)
- Fraud detection (ML pipeline)

**Key characteristics**:
- Events are **retained** (can replay history)
- Multiple **consumer groups** read same events
- High throughput (millions/sec)
- "This happened" pattern (observation)

```java
@ServiceActivator(inputChannel = "auditChannel")
public void publishAuditEvent(Claim claim) {
    // In production: eventHubsTemplate.send("claim-events", event, claim.getClaimId())
    log.info("EVENT HUB: Publishing audit event for claim {}", claim.getClaimId());
}
```

**Interview answer**: "I use Event Hubs when multiple consumers need to observe the same events. For claims processing, the analytics dashboard, audit system, and fraud detection all need to see every claim - they each have their own consumer group reading from the same stream."

---

### Service Bus (`ServiceBusProducer.java`)

**What it is**: Enterprise message broker for reliable messaging.

**Use cases**:
- Payment processing (exactly-once)
- Workflow orchestration
- Notifications

**Key characteristics**:
- Message consumed **once** then removed
- **Dead Letter Queue** for failures
- Transactions supported
- "Do this" pattern (command)

```java
@ServiceActivator(inputChannel = "paymentChannel")
public void sendToPaymentQueue(Claim claim) {
    if (claim.getStatus() != ClaimStatus.APPROVED) return;  // Only approved claims

    // In production: serviceBusSender.sendMessage(message)
    log.info("SERVICE BUS: Sending claim {} for payment", claim.getClaimId());
}
```

**Interview answer**: "I use Service Bus for commands that need exactly-once processing, like payments. Each message is processed by one consumer and removed. If processing fails, it goes to the Dead Letter Queue where ops can investigate and resubmit."

---

### Dead Letter Queue (`DeadLetterHandler.java`)

**What it is**: Where failed messages go for investigation.

**Why it matters**: You never want to lose messages. Failed ones need manual review.

**Messages go to DLQ when**:
- Max delivery attempts exceeded
- Message expired (TTL)
- Consumer explicitly dead-letters it
- Poison message (can't deserialize)

```java
public void handleDeadLetter(Map<String, Object> message, String reason) {
    log.error("DEAD LETTER: Claim {} failed - {}", message.get("claimId"), reason);

    // In production:
    // 1. Store in failed_payments table
    // 2. Alert ops team
    // 3. Create incident ticket
}
```

**Interview answer**: "I always configure dead-letter queues for critical workflows. If a payment fails, it goes to the DLQ with the failure reason. Ops investigates, fixes the issue, and resubmits. We never lose a payment."

---

### When to Use Each

| Scenario | Service Bus | Event Hubs |
|----------|-------------|------------|
| Process a payment | ✅ Queue | |
| Notify judge AND prosecutor | ✅ Topic (both subscribe) | |
| Audit trail | | ✅ Stream |
| Real-time dashboard | | ✅ Consumer group |
| Fraud detection ML | | ✅ Consumer group |
| Exactly-once delivery | ✅ | |
| Replay last week's events | | ✅ Retention |
| High volume (millions/sec) | | ✅ |

---

## Phase 3 Files

| File | Purpose |
|------|---------|
| `EventHubProducer.java` | Publishes to Event Hubs for audit |
| `ServiceBusProducer.java` | Sends approved claims for payment |
| `DeadLetterHandler.java` | Handles failed messages |

---

# Complete File Summary

## By Category

### Domain Models (3 files)
```
model/
├── Claim.java           # Core domain object
├── ClaimType.java       # AUTO, PROPERTY, HEALTH, LIFE, LIABILITY
└── ClaimStatus.java     # SUBMITTED → VALIDATED → PROCESSING → APPROVED
```

### DTOs (4 files)
```
dto/
├── ClaimRequest.java    # JSON input from API
├── ClaimResponse.java   # API response
├── CsvClaimRecord.java  # CSV row for batch
└── BatchSummary.java    # Aggregation result
```

### Integration Components (14 files)
```
integration/
├── gateway/
│   ├── ClaimGateway.java          # Single claim entry
│   └── BatchClaimGateway.java     # Batch entry
├── channels/
│   └── ClaimChannels.java         # All channel definitions
├── transformers/
│   └── ClaimTransformer.java      # Enrich claims
├── filters/
│   └── ClaimValidationFilter.java # Validate claims
├── routers/
│   └── ClaimTypeRouter.java       # Route by type
├── splitters/
│   └── BatchClaimSplitter.java    # CSV → claims
├── aggregators/
│   └── BatchResultAggregator.java # Claims → summary
├── handlers/
│   ├── AutoClaimHandler.java
│   ├── PropertyClaimHandler.java
│   ├── HealthClaimHandler.java
│   ├── HighValueClaimHandler.java
│   └── InvalidClaimHandler.java
└── flows/
    ├── ClaimIntakeFlow.java       # Main flow
    └── BatchProcessingFlow.java   # Batch flow
```

### Messaging (3 files)
```
messaging/
├── EventHubProducer.java    # Audit events → Event Hubs
├── ServiceBusProducer.java  # Payments → Service Bus
└── DeadLetterHandler.java   # Failed message handling
```

### Controller (1 file)
```
controller/
└── ClaimController.java     # REST API endpoints
```

### Configuration (2 files)
```
resources/
├── application.yml      # Spring config
└── sample-claims.csv    # Test data
```

---

# How to Run

```bash
# Navigate to project
cd D:/kaizen_ws/claims-integration-hub

# Build (requires Maven)
mvn clean package

# Run
mvn spring-boot:run
# OR
java -jar target/claims-integration-hub-0.0.1-SNAPSHOT.jar

# The app runs on port 8081
```

## Test Endpoints

### Single Claim
```bash
curl -X POST http://localhost:8081/api/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-123456",
    "claimType": "AUTO",
    "amount": 2500.00,
    "description": "Rear-end collision",
    "incidentDate": "2024-01-15",
    "claimantName": "John Doe",
    "claimantEmail": "john@email.com"
  }'
```

### High-Value Claim (triggers fraud review)
```bash
curl -X POST http://localhost:8081/api/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-789",
    "claimType": "PROPERTY",
    "amount": 50000.00,
    "description": "Fire damage",
    "incidentDate": "2024-01-20",
    "claimantName": "Jane Smith",
    "claimantEmail": "jane@email.com"
  }'
```

### Batch (CSV)
```bash
curl -X POST http://localhost:8081/api/claims/batch \
  -F "file=@src/main/resources/sample-claims.csv"
```

---

# Interview Preparation

## Key Questions and Answers

### "Tell me about your Spring Integration experience"

> "I built a claims integration hub as a learning project. It accepts claims via REST API or CSV batch files, validates and transforms them, routes by claim type to specialized handlers, and sends approved claims to a payment queue while publishing all events to an audit stream.
>
> I used Spring Integration patterns throughout - Gateway for clean entry points, Transformer for data normalization, Filter for validation, Router for content-based routing, Splitter for batch processing, and Aggregator to combine results. For Azure integration, I used Event Hubs for the audit trail and Service Bus for payment processing."

### "When would you use Filter vs Router?"

> "Filter is for binary YES/NO decisions - should this message continue or be rejected? Router is for routing to one of multiple destinations based on content. In my project, Filter validates claims (valid or invalid), while Router directs valid claims to different handlers based on type."

### "How do you handle batch processing?"

> "I use Splitter to break a batch file into individual messages, each processed independently. The Aggregator recombines them using correlation IDs. The tricky part is the release strategy - knowing when all messages are received. I use sequenceSize headers and a timeout fallback for partial results."

### "Service Bus vs Event Hubs?"

> "Service Bus is for commands - 'do this task.' Messages are consumed once and removed. I use it for payment processing where exactly-once matters.
>
> Event Hubs is for events - 'this happened.' Events are retained and multiple consumers can read the same stream. I use it for audit trails where compliance, analytics, and fraud detection all need to see every claim."

### "How do you handle failures?"

> "For message failures, I use Dead Letter Queues. Failed messages go there with the failure reason for investigation. For validation failures, I configure discardChannel on filters so rejected messages aren't lost - they go to error handling."

---

# Next Steps

1. **Read through each file** - Comments explain every pattern
2. **Run locally and test** - See the logs to understand message flow
3. **Trace a single claim** - Follow it through Gateway → Transformer → Filter → Router → Handler
4. **Trace a batch** - See Splitter break it apart and Aggregator combine results
5. **Practice explaining** - Talk through the architecture out loud
