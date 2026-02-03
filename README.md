# Claims Integration Hub

A Spring Integration learning project demonstrating Enterprise Integration Patterns (EIP) with Azure messaging.

## Purpose

This project was built to learn and demonstrate:
- Spring Integration patterns (Gateway, Transformer, Filter, Router, Splitter, Aggregator, Service Activator)
- Azure Service Bus for reliable message queuing
- Azure Event Hubs for event streaming
- Real-world integration architecture for insurance claims processing

## Full Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CLAIMS INTEGRATION HUB                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  POST /api/claims (JSON)              POST /api/claims/batch (CSV)           │
│         │                                      │                             │
│         ▼                                      ▼                             │
│    ┌─────────┐                           ┌───────────┐                       │
│    │ GATEWAY │                           │ SPLITTER  │ ← Break into records  │
│    └────┬────┘                           └─────┬─────┘                       │
│         │                                      │                             │
│         │         ┌────────────────────────────┘                             │
│         │         │                                                          │
│         ▼         ▼                                                          │
│    ┌─────────────────┐                                                       │
│    │   TRANSFORMER   │ ← Enrich with metadata                                │
│    └────────┬────────┘                                                       │
│             │                                                                │
│             ▼                                                                │
│        ┌────────┐                                                            │
│        │ FILTER │ ← Validate required fields                                 │
│        └────┬───┘                                                            │
│             │                                                                │
│        ┌────┴────┐                                                           │
│        ▼         ▼                                                           │
│      Valid    Invalid → invalidClaimsChannel → InvalidClaimHandler           │
│        │                                                                     │
│        ▼                                                                     │
│    ┌────────┐                                                                │
│    │ ROUTER │ ← Route by claim type                                          │
│    └────┬───┘                                                                │
│         │                                                                    │
│    ┌────┼────────────┬────────────┬──────────────┐                          │
│    ▼    ▼            ▼            ▼              ▼                          │
│  AUTO PROPERTY   HEALTH      GENERAL      HIGH-VALUE                         │
│    │    │            │            │              │                          │
│    └────┴────────────┴────────────┴──────────────┘                          │
│                        │                                                     │
│                        ▼                                                     │
│              ┌─────────────────┐                                             │
│              │ SERVICE         │ ← Business logic                            │
│              │ ACTIVATORS      │                                             │
│              └────────┬────────┘                                             │
│                       │                                                      │
│                       ▼                                                      │
│              ┌─────────────────┐         ┌─────────────────┐                │
│              │  AGGREGATOR     │         │ Processed Claims │                │
│              │  (batch only)   │────────►│    Channel       │                │
│              └─────────────────┘         └────────┬────────┘                │
│                                                   │                          │
│                        ┌──────────────────────────┴──────────────────┐       │
│                        │              PUBLISH-SUBSCRIBE              │       │
│                        │    (same claim goes to both channels)       │       │
│                        └──────────────────┬───────────────────┬──────┘       │
│                                           │                   │              │
│                                           ▼                   ▼              │
│                                   ┌─────────────┐     ┌─────────────┐        │
│                                   │ auditChannel│     │paymentChannel│       │
│                                   └──────┬──────┘     └──────┬──────┘        │
│                                          │                   │               │
└──────────────────────────────────────────┼───────────────────┼───────────────┘
                                           │                   │
                                           ▼                   ▼
                                  ┌─────────────────┐  ┌─────────────────┐
                                  │  EVENT HUBS     │  │  SERVICE BUS    │
                                  │  "claim-events" │  │  "payment-queue"│
                                  │                 │  │                 │
                                  │  Consumer Groups│  │  Dead Letter    │
                                  │  • analytics-cg │  │  Queue for      │
                                  │  • audit-cg     │  │  failed         │
                                  │  • fraud-cg     │  │  payments       │
                                  └─────────────────┘  └─────────────────┘
```

## Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) Azure subscription for cloud deployment

## Running Locally

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/claims-integration-hub-0.0.1-SNAPSHOT.jar
```

## Testing the API

### Submit a Single Claim (JSON)

```bash
curl -X POST http://localhost:8081/api/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-123456",
    "claimType": "AUTO",
    "amount": 2500.00,
    "description": "Rear-end collision at intersection",
    "incidentDate": "2024-01-15",
    "claimantName": "John Doe",
    "claimantEmail": "john.doe@email.com"
  }'
```

### Submit a High-Value Claim (triggers fraud review)

```bash
curl -X POST http://localhost:8081/api/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-789012",
    "claimType": "PROPERTY",
    "amount": 50000.00,
    "description": "Fire damage to kitchen",
    "incidentDate": "2024-01-20",
    "claimantName": "Jane Smith",
    "claimantEmail": "jane.smith@email.com"
  }'
```

### Submit a Batch (CSV)

```bash
# Using file upload
curl -X POST http://localhost:8081/api/claims/batch \
  -F "file=@src/main/resources/sample-claims.csv"

# Using raw CSV content
curl -X POST http://localhost:8081/api/claims/batch/raw \
  -H "Content-Type: text/plain" \
  -d 'policyNumber,claimType,amount,description,incidentDate,claimantName,claimantEmail
POL-001,AUTO,2500.00,Fender bender,2024-01-15,John Doe,john@email.com
POL-002,HEALTH,500.00,Doctor visit,2024-01-16,Jane Smith,jane@email.com'
```

### Check Health

```bash
curl http://localhost:8081/api/claims/health
```

## Project Structure

```
src/main/java/com/example/claims/
├── ClaimsIntegrationHubApplication.java
│
├── model/                           # Domain models
│   ├── Claim.java
│   ├── ClaimType.java               # AUTO, PROPERTY, HEALTH, LIFE, LIABILITY
│   └── ClaimStatus.java             # SUBMITTED → VALIDATED → PROCESSING → APPROVED
│
├── dto/                             # Data Transfer Objects
│   ├── ClaimRequest.java            # JSON input
│   ├── ClaimResponse.java           # API response
│   ├── CsvClaimRecord.java          # CSV batch input
│   └── BatchSummary.java            # Aggregation output
│
├── integration/                     # Spring Integration components
│   ├── gateway/
│   │   ├── ClaimGateway.java        # Single claim entry point
│   │   └── BatchClaimGateway.java   # Batch entry point
│   │
│   ├── channels/
│   │   └── ClaimChannels.java       # Message channel definitions
│   │
│   ├── transformers/
│   │   └── ClaimTransformer.java    # Enrich claims with metadata
│   │
│   ├── filters/
│   │   └── ClaimValidationFilter.java  # Validate or reject
│   │
│   ├── routers/
│   │   └── ClaimTypeRouter.java     # Route by claim type
│   │
│   ├── splitters/
│   │   └── BatchClaimSplitter.java  # Break CSV into claims
│   │
│   ├── aggregators/
│   │   └── BatchResultAggregator.java  # Combine into summary
│   │
│   ├── handlers/                    # Service Activators
│   │   ├── AutoClaimHandler.java
│   │   ├── PropertyClaimHandler.java
│   │   ├── HealthClaimHandler.java
│   │   ├── HighValueClaimHandler.java
│   │   └── InvalidClaimHandler.java
│   │
│   └── flows/
│       ├── ClaimIntakeFlow.java     # Main processing flow
│       └── BatchProcessingFlow.java # Batch flow with aggregator
│
├── messaging/                       # Azure messaging
│   ├── EventHubProducer.java        # Audit trail (Event Hubs)
│   ├── ServiceBusProducer.java      # Payment queue (Service Bus)
│   └── DeadLetterHandler.java       # Failed message handling
│
└── controller/
    └── ClaimController.java         # REST API endpoints
```

## EIP Patterns Demonstrated

| Pattern | File | Purpose |
|---------|------|---------|
| **Gateway** | `ClaimGateway.java` | Clean API hiding messaging complexity |
| **Channel** | `ClaimChannels.java` | Decouple producers from consumers |
| **Transformer** | `ClaimTransformer.java` | Convert/enrich message payloads |
| **Filter** | `ClaimValidationFilter.java` | Pass or reject messages |
| **Router** | `ClaimTypeRouter.java` | Route to different channels based on content |
| **Splitter** | `BatchClaimSplitter.java` | Break one message into many |
| **Aggregator** | `BatchResultAggregator.java` | Combine many messages into one |
| **Service Activator** | `*ClaimHandler.java` | Invoke business logic |

## Azure Messaging

### Event Hubs (for audit/analytics)

- **Use case**: All processed claims published for audit trail
- **Consumer groups**: analytics, audit, fraud detection all read same stream
- **Retention**: Events kept for replay
- **Pattern**: "This happened" (observe events)

### Service Bus (for payments)

- **Use case**: Approved claims sent for payment processing
- **Delivery**: Exactly-once (message removed after processing)
- **Dead Letter Queue**: Failed payments captured for manual review
- **Pattern**: "Do this" (command)

### When to Use Each

| Use Case | Service Bus | Event Hubs |
|----------|-------------|------------|
| Process a payment | ✅ Queue | |
| Notify multiple parties | ✅ Topic | |
| Audit trail | | ✅ Stream |
| Real-time dashboard | | ✅ Consumer group |
| Fraud detection | | ✅ Consumer group |
| Exactly-once delivery | ✅ | |
| Replay historical events | | ✅ |

## Interview Topics Covered

This project demonstrates knowledge of:

1. **Spring Integration** - All major EIP patterns
2. **Enterprise Integration Patterns** - When and why to use each
3. **Azure Service Bus** - Queues, topics, dead-letter queues
4. **Azure Event Hubs** - Streaming, partitions, consumer groups
5. **Message-driven architecture** - Async, decoupled, scalable
6. **Data transformation** - JSON, XML, CSV normalization
7. **Error handling** - Invalid claims, dead letters, retries
8. **Idempotency** - Using claim ID to prevent duplicate payments

## Key Learnings

### Filter vs Router
- **Filter**: YES/NO decision (pass or block)
- **Router**: WHICH ONE? (multiple destinations)

### Splitter + Aggregator
- Always correlated - messages share correlation ID
- Aggregator needs release strategy (when to combine)
- Handle timeouts - what if one message never arrives?

### Dead Letter Queues
- Never lose messages
- Include failure reason
- Manual investigation and retry
