package com.example.claims.messaging;

import com.example.claims.model.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure Event Hubs Producer
 *
 * Sends claim events to Event Hubs for:
 * - Audit trail (compliance requirement)
 * - Analytics dashboard (real-time metrics)
 * - Fraud detection (ML pipeline)
 *
 * EVENT HUBS vs SERVICE BUS:
 *
 * Event Hubs is for EVENTS ("this happened"):
 * - High throughput (millions/sec)
 * - Events are RETAINED (can replay)
 * - Multiple consumers read SAME events
 * - Used for: logging, analytics, audit, streaming
 *
 * Interview tip: "I use Event Hubs when I need multiple consumers
 * to observe the same events, like feeding both an analytics
 * dashboard and a fraud detection system from the same claim stream."
 */
@Component
@Slf4j
public class EventHubProducer {

    @Value("${azure.eventhubs.enabled:false}")
    private boolean eventHubsEnabled;

    private final ObjectMapper objectMapper;

    public EventHubProducer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Publish a claim event to Event Hubs.
     *
     * In production with Azure SDK, this would use:
     * - EventHubProducerClient
     * - Or Spring Cloud Azure EventHubsTemplate
     *
     * Events are partitioned by claimId for ordering within a claim.
     */
    @ServiceActivator(inputChannel = "auditChannel")
    public void publishAuditEvent(Claim claim) {
        Map<String, Object> event = buildEvent(claim, "CLAIM_PROCESSED");

        if (eventHubsEnabled) {
            // In production: eventHubsTemplate.send("claim-events", event, claim.getClaimId())
            log.info("EVENT HUB [claim-events]: Publishing event for claim {}",
                    claim.getClaimId());
            sendToEventHub("claim-events", event, claim.getClaimId());
        } else {
            // Local development - just log
            log.info("════════════════════════════════════════════════════════════════");
            log.info("📡 EVENT HUB (simulated) - Audit Event");
            log.info("════════════════════════════════════════════════════════════════");
            log.info("  Event Hub: claim-events");
            log.info("  Partition Key: {}", claim.getClaimId());
            log.info("  Event Type: CLAIM_PROCESSED");
            log.info("  Claim ID: {}", claim.getClaimId());
            log.info("  Status: {}", claim.getStatus());
            log.info("  Amount: ${}", claim.getAmount());
            log.info("  Timestamp: {}", Instant.now());
            log.info("════════════════════════════════════════════════════════════════");
        }
    }

    /**
     * Publish multiple event types for different lifecycle stages.
     */
    public void publishEvent(Claim claim, String eventType) {
        Map<String, Object> event = buildEvent(claim, eventType);

        log.info("📡 EVENT HUB: {} for claim {}", eventType, claim.getClaimId());

        if (eventHubsEnabled) {
            sendToEventHub("claim-events", event, claim.getClaimId());
        } else {
            logEvent(event);
        }
    }

    /**
     * Build the event payload.
     */
    private Map<String, Object> buildEvent(Claim claim, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("timestamp", Instant.now().toString());
        event.put("claimId", claim.getClaimId());
        event.put("policyNumber", claim.getPolicyNumber());
        event.put("claimType", claim.getClaimType().name());
        event.put("status", claim.getStatus().name());
        event.put("amount", claim.getAmount());
        event.put("source", claim.getSource());
        event.put("correlationId", claim.getCorrelationId());
        event.put("flaggedForReview", claim.isFlaggedForReview());
        return event;
    }

    /**
     * Send to Event Hub (placeholder for actual Azure SDK call).
     *
     * In production, inject EventHubsTemplate and use:
     *   eventHubsTemplate.send(eventHubName, event, partitionKey)
     */
    private void sendToEventHub(String eventHubName, Map<String, Object> event, String partitionKey) {
        try {
            String json = objectMapper.writeValueAsString(event);
            // In production:
            // eventHubsTemplate.send(eventHubName, json, partitionKey);
            log.debug("Would send to Event Hub {}: {}", eventHubName, json);
        } catch (JsonProcessingException e) {
            log.error("Error serializing event: {}", e.getMessage());
        }
    }

    private void logEvent(Map<String, Object> event) {
        try {
            log.debug("Event payload: {}", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.debug("Event payload: {}", event);
        }
    }
}
