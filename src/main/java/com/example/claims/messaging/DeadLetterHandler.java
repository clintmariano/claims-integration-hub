package com.example.claims.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dead Letter Queue Handler
 *
 * Handles messages that failed processing and ended up in DLQ.
 *
 * DEAD LETTER QUEUE (DLQ) is critical for:
 * - Not losing messages that fail processing
 * - Manual investigation of failures
 * - Retry after fixing issues
 * - Audit trail of problems
 *
 * Messages go to DLQ when:
 * 1. Max delivery attempts exceeded
 * 2. Message expired (TTL)
 * 3. Explicit dead-lettering by consumer
 * 4. Poison message (can't be deserialized)
 *
 * Interview tip: "I always configure dead-letter queues for critical
 * workflows. If a payment fails, I don't want to lose it - it goes
 * to the DLQ where ops can investigate and resubmit after fixing
 * the issue. The DLQ message includes the failure reason."
 */
@Component
@Slf4j
public class DeadLetterHandler {

    /**
     * Process a dead-lettered message.
     *
     * In production with Azure SDK, this would be:
     * @ServiceBusListener(destination = "payment-queue/$deadletterqueue")
     *
     * The dead-letter message includes:
     * - Original message content
     * - DeadLetterReason header
     * - DeadLetterErrorDescription header
     * - EnqueuedTimeUtc (when it was dead-lettered)
     */
    public void handleDeadLetter(Map<String, Object> message,
                                  String deadLetterReason,
                                  String deadLetterDescription) {

        log.error("════════════════════════════════════════════════════════════════");
        log.error("☠️  DEAD LETTER RECEIVED");
        log.error("════════════════════════════════════════════════════════════════");
        log.error("  Reason: {}", deadLetterReason);
        log.error("  Description: {}", deadLetterDescription);
        log.error("  Message ID: {}", message.get("messageId"));
        log.error("  Claim ID: {}", message.get("claimId"));
        log.error("  Original Queue: payment-queue");
        log.error("════════════════════════════════════════════════════════════════");

        // In production, you would:
        // 1. Store in a failed_payments table
        // 2. Send alert to ops team
        // 3. Create incident ticket
        // 4. Log for compliance

        handleFailedPayment(message, deadLetterReason);
    }

    /**
     * Handle a failed payment specifically.
     */
    private void handleFailedPayment(Map<String, Object> message, String reason) {
        String claimId = (String) message.get("claimId");

        log.error("  ACTION REQUIRED:");
        log.error("  1. Investigate claim {} in admin portal", claimId);
        log.error("  2. Check for data issues or system problems");
        log.error("  3. Fix the issue");
        log.error("  4. Resubmit from DLQ or create manual payment");

        // Determine if it's retryable
        boolean isRetryable = isRetryableError(reason);
        if (isRetryable) {
            log.warn("  This error may be retryable. Consider automatic retry after delay.");
        } else {
            log.error("  This error requires manual intervention.");
        }
    }

    /**
     * Determine if an error is retryable.
     *
     * Some errors (like network timeout) can be retried.
     * Others (like invalid data) need manual fix.
     */
    private boolean isRetryableError(String reason) {
        if (reason == null) return false;

        // Retryable errors
        if (reason.contains("timeout") ||
            reason.contains("connection") ||
            reason.contains("throttled") ||
            reason.contains("busy")) {
            return true;
        }

        // Non-retryable errors
        // - InvalidData
        // - ValidationFailed
        // - DuplicateDetected
        return false;
    }

    /**
     * Resubmit a dead-lettered message back to the original queue.
     *
     * Only do this after fixing the underlying issue!
     */
    public void resubmitMessage(Map<String, Object> message, String originalQueue) {
        log.info("Resubmitting message {} to queue {}",
                message.get("messageId"), originalQueue);

        // In production:
        // 1. Create new message from dead-letter content
        // 2. Set new message ID (to avoid duplicate detection)
        // 3. Increment retry count header
        // 4. Send to original queue
        // 5. Delete from DLQ (complete the dead-letter message)
    }
}
