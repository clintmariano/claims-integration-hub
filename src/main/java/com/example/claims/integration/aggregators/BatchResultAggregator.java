package com.example.claims.integration.aggregators;

import com.example.claims.dto.BatchSummary;
import com.example.claims.model.Claim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Batch Result Aggregator
 *
 * PATTERN: Aggregator
 * PURPOSE: Combine multiple messages into one message
 *
 * What it does:
 * - Receives individual processed claims
 * - Groups them by correlation ID (batch ID)
 * - When all claims in a batch are received, combines into summary
 *
 * Key concepts:
 *
 * 1. CORRELATION STRATEGY: How to group messages together
 *    - Same correlation ID = same group
 *    - Messages from the same Splitter share a correlation ID
 *
 * 2. RELEASE STRATEGY: When to release the group
 *    - When all messages received (sequenceSize == group size)
 *    - Or after a timeout (don't wait forever)
 *
 * 3. OUTPUT PROCESSOR: What to produce when releasing
 *    - Takes all messages in group
 *    - Produces single output (the summary)
 *
 * Interview tip: "Aggregator is the opposite of Splitter.
 * It uses correlation to group related messages, and a release
 * strategy to know when the group is complete. The tricky part
 * is handling timeouts - what if one message never arrives?"
 */
@Component
@Slf4j
public class BatchResultAggregator {

    /**
     * Correlation Strategy: How to group messages together.
     *
     * Messages with the same correlation value go into the same group.
     * For batch processing, we correlate by batchId.
     *
     * @param message The incoming message
     * @return The correlation key (messages with same key are grouped)
     */
    @CorrelationStrategy
    public String correlateByBatchId(Message<Claim> message) {
        Claim claim = message.getPayload();
        String correlationId = claim.getCorrelationId();
        log.debug("Correlating claim {} to batch {}", claim.getClaimId(), correlationId);
        return correlationId;
    }

    /**
     * Release Strategy: When to release the group.
     *
     * Options:
     * 1. When all messages received (check sequenceSize header)
     * 2. After a timeout (configured separately)
     * 3. When some condition is met (e.g., 10 messages)
     *
     * @param messages All messages in the group so far
     * @return true if group should be released
     */
    @ReleaseStrategy
    public boolean canRelease(List<Message<Claim>> messages) {
        if (messages.isEmpty()) {
            return false;
        }

        // Get expected size from first message's header
        Message<Claim> first = messages.get(0);
        Integer sequenceSize = first.getHeaders().get("sequenceSize", Integer.class);

        if (sequenceSize == null) {
            // No sequence info - release after reasonable count
            log.debug("No sequence size, releasing after {} messages", messages.size());
            return messages.size() >= 10;
        }

        boolean canRelease = messages.size() >= sequenceSize;
        log.debug("Release check: have {} of {} messages, canRelease={}",
                messages.size(), sequenceSize, canRelease);
        return canRelease;
    }

    /**
     * Aggregator: Combine all messages into a summary.
     *
     * Called when the release strategy returns true.
     * Receives ALL messages in the group.
     * Returns a SINGLE message (the aggregated result).
     *
     * @param claims All claims in the batch (after processing)
     * @return Summary of the batch
     */
    @Aggregator(inputChannel = "aggregationChannel", outputChannel = "batchSummaryChannel")
    public BatchSummary aggregateBatch(List<Claim> claims) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("AGGREGATOR: Combining {} claims into summary", claims.size());
        log.info("═══════════════════════════════════════════════════════════════");

        String batchId = claims.isEmpty() ? UUID.randomUUID().toString()
                : claims.get(0).getCorrelationId();

        BatchSummary summary = BatchSummary.builder()
                .batchId(batchId)
                .startTime(claims.stream()
                        .map(Claim::getReceivedAt)
                        .min(Instant::compareTo)
                        .orElse(Instant.now()))
                .endTime(Instant.now())
                .totalClaims(claims.size())
                .build();

        // Calculate statistics
        int validCount = 0;
        int invalidCount = 0;
        int flaggedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Claim claim : claims) {
            // Count by type
            summary.incrementType(claim.getClaimType().name());

            // Count by status
            summary.incrementStatus(claim.getStatus().name());

            // Valid vs invalid
            if (claim.getStatus() == com.example.claims.model.ClaimStatus.INVALID) {
                invalidCount++;
            } else {
                validCount++;
            }

            // Flagged for review
            if (claim.isFlaggedForReview()) {
                flaggedCount++;
            }

            // Total amount
            if (claim.getAmount() != null) {
                totalAmount = totalAmount.add(claim.getAmount());
            }
        }

        summary.setValidClaims(validCount);
        summary.setInvalidClaims(invalidCount);
        summary.setFlaggedForReview(flaggedCount);
        summary.setTotalAmount(totalAmount);

        log.info("AGGREGATOR: Batch {} summary:", batchId);
        log.info("  Total Claims: {}", summary.getTotalClaims());
        log.info("  Valid: {}, Invalid: {}", summary.getValidClaims(), summary.getInvalidClaims());
        log.info("  By Type: {}", summary.getClaimsByType());
        log.info("  By Status: {}", summary.getClaimsByStatus());
        log.info("  Total Amount: ${}", summary.getTotalAmount());
        log.info("  Flagged for Review: {}", summary.getFlaggedForReview());

        return summary;
    }
}
