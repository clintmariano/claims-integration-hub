package com.example.claims.messaging;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
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
import java.util.UUID;

/**
 * Azure Service Bus Producer
 *
 * Sends approved claims to Service Bus queue for payment processing.
 *
 * SERVICE BUS vs EVENT HUBS:
 *
 * Service Bus is for COMMANDS ("do this"):
 * - Guaranteed delivery
 * - Message consumed ONCE then removed
 * - Supports transactions
 * - Dead-letter queue for failures
 * - Used for: workflows, payments, tasks
 *
 * Queue vs Topic:
 * - QUEUE: One consumer gets each message (competing consumers)
 * - TOPIC: Multiple subscriptions each get a copy (pub/sub)
 *
 * Interview tip: "I use Service Bus queues when I need exactly-once
 * processing, like payment processing where we can't process the
 * same claim twice. The dead-letter queue catches any failures
 * for manual review."
 */
@Component
@Slf4j
public class ServiceBusProducer {

    @Value("${azure.servicebus.enabled:false}")
    private boolean serviceBusEnabled;

    private final ObjectMapper objectMapper;

    public ServiceBusProducer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Send approved claim to payment queue.
     *
     * Only APPROVED claims go to the payment queue.
     * This is checked before sending.
     */
    @ServiceActivator(inputChannel = "paymentChannel")
    public void sendToPaymentQueue(Claim claim) {
        // Only send approved claims to payment
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            log.debug("Skipping payment queue - claim {} status is {}",
                    claim.getClaimId(), claim.getStatus());
            return;
        }

        Map<String, Object> message = buildPaymentMessage(claim);

        if (serviceBusEnabled) {
            // In production: serviceBusSenderClient.sendMessage(...)
            log.info("SERVICE BUS [payment-queue]: Sending claim {} for payment (${}))",
                    claim.getClaimId(), claim.getAmount());
            sendToQueue("payment-queue", message, claim.getClaimId());
        } else {
            // Local development - simulate
            log.info("════════════════════════════════════════════════════════════════");
            log.info("💳 SERVICE BUS (simulated) - Payment Queue");
            log.info("════════════════════════════════════════════════════════════════");
            log.info("  Queue: payment-queue");
            log.info("  Message ID: {}", message.get("messageId"));
            log.info("  Claim ID: {}", claim.getClaimId());
            log.info("  Policy: {}", claim.getPolicyNumber());
            log.info("  Amount: ${}", claim.getAmount());
            log.info("  Payee: {}", claim.getClaimantName());
            log.info("  Email: {}", claim.getClaimantEmail());
            log.info("════════════════════════════════════════════════════════════════");
            log.info("  ℹ️  In production, this would be picked up by Payment Service");
            log.info("  ℹ️  If processing fails, message goes to Dead Letter Queue");
            log.info("════════════════════════════════════════════════════════════════");
        }
    }

    /**
     * Build the payment message payload.
     *
     * Include all information needed by the payment system.
     * Use idempotency key to prevent duplicate payments.
     */
    private Map<String, Object> buildPaymentMessage(Claim claim) {
        Map<String, Object> message = new HashMap<>();

        // Message metadata
        message.put("messageId", UUID.randomUUID().toString());
        message.put("idempotencyKey", claim.getClaimId()); // Prevent duplicate payments!
        message.put("timestamp", Instant.now().toString());

        // Payment details
        message.put("claimId", claim.getClaimId());
        message.put("policyNumber", claim.getPolicyNumber());
        message.put("paymentAmount", claim.getAmount());
        message.put("payeeName", claim.getClaimantName());
        message.put("payeeEmail", claim.getClaimantEmail());
        message.put("claimType", claim.getClaimType().name());

        // For audit
        message.put("correlationId", claim.getCorrelationId());
        message.put("source", claim.getSource());

        return message;
    }

    /**
     * Send to Service Bus queue (placeholder for actual Azure SDK call).
     *
     * In production, inject ServiceBusSenderClient and use:
     *   serviceBusSenderClient.sendMessage(message)
     *
     * Key features:
     * - Session support (for ordered processing)
     * - Scheduled delivery
     * - Dead letter queue on failure
     */
    private void sendToQueue(String queueName, Map<String, Object> message, String sessionId) {
        try {
            String json = objectMapper.writeValueAsString(message);

            // In production with Azure SDK:
            // ServiceBusMessage sbMessage = new ServiceBusMessage(json)
            //     .setMessageId(message.get("messageId").toString())
            //     .setSessionId(sessionId)  // For ordered processing
            //     .setContentType("application/json");
            // serviceBusSenderClient.sendMessage(sbMessage);

            log.debug("Would send to Service Bus queue {}: {}", queueName, json);
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage());
        }
    }

    /**
     * Send to Service Bus topic (for notifications).
     *
     * Topics support multiple subscriptions with filters.
     * Example: notify both adjuster AND claimant about status change.
     */
    public void sendToTopic(String topicName, Claim claim, String eventType) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("eventType", eventType);
        message.put("claimId", claim.getClaimId());
        message.put("status", claim.getStatus().name());
        message.put("timestamp", Instant.now().toString());

        log.info("SERVICE BUS [topic: {}]: {} for claim {}",
                topicName, eventType, claim.getClaimId());

        // In production:
        // serviceBusSenderClient.sendMessage(topicName, message);
    }
}
