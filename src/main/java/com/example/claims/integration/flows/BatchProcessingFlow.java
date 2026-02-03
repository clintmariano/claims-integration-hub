package com.example.claims.integration.flows;

import com.example.claims.dto.BatchSummary;
import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

/**
 * Batch Processing Integration Flow
 *
 * Demonstrates SPLITTER and AGGREGATOR patterns.
 *
 * Flow:
 * 1. CSV content arrives at batchInputChannel
 * 2. Splitter breaks into individual claims (batchProcessingChannel)
 * 3. Each claim is processed (simulated)
 * 4. Claims are sent to aggregationChannel
 * 5. Aggregator combines into BatchSummary
 * 6. Summary sent to batchSummaryChannel
 */
@Configuration
@Slf4j
public class BatchProcessingFlow {

    // ═══════════════════════════════════════════════════════════════════════
    // CHANNELS
    // ═══════════════════════════════════════════════════════════════════════

    @Bean
    public MessageChannel batchInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel batchProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel aggregationChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel batchSummaryChannel() {
        return new DirectChannel();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FLOWS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process each split claim and send to aggregation.
     *
     * This simulates the full claim processing pipeline for batch items.
     * In a real system, you might route these through the full flow.
     */
    @Bean
    public IntegrationFlow batchClaimProcessingFlow() {
        return IntegrationFlow
                .from("batchProcessingChannel")
                .handle(Claim.class, (claim, headers) -> {
                    log.info("Processing batch claim: {} (Policy: {}, Type: {})",
                            claim.getClaimId(),
                            claim.getPolicyNumber(),
                            claim.getClaimType());

                    // Simulate processing based on type
                    switch (claim.getClaimType()) {
                        case AUTO:
                            claim.setStatus(claim.getAmount().compareTo(
                                    new java.math.BigDecimal("1000")) < 0
                                    ? ClaimStatus.APPROVED
                                    : ClaimStatus.PENDING_REVIEW);
                            break;
                        case PROPERTY:
                            claim.setStatus(ClaimStatus.PENDING_REVIEW);
                            break;
                        case HEALTH:
                            claim.setStatus(ClaimStatus.PROCESSING);
                            break;
                        default:
                            claim.setStatus(ClaimStatus.PROCESSING);
                    }

                    // Flag high-value claims
                    if (claim.isHighValue()) {
                        claim.setFlaggedForReview(true);
                        claim.setStatus(ClaimStatus.PENDING_REVIEW);
                    }

                    log.info("Claim {} processed with status: {}",
                            claim.getClaimId(), claim.getStatus());

                    return claim;
                })
                .channel("aggregationChannel")
                .get();
    }

    /**
     * Handle the final batch summary.
     *
     * In a real system, this might:
     * - Store summary in database
     * - Send email notification
     * - Trigger downstream processes
     */
    @Bean
    public IntegrationFlow batchSummaryHandlingFlow() {
        return IntegrationFlow
                .from("batchSummaryChannel")
                .handle(BatchSummary.class, (summary, headers) -> {
                    log.info("════════════════════════════════════════════════════════════════");
                    log.info("✅ BATCH PROCESSING COMPLETE");
                    log.info("════════════════════════════════════════════════════════════════");
                    log.info("Batch ID: {}", summary.getBatchId());
                    log.info("Duration: {} ms",
                            summary.getEndTime().toEpochMilli() - summary.getStartTime().toEpochMilli());
                    log.info("Total Claims: {}", summary.getTotalClaims());
                    log.info("Valid: {} | Invalid: {}",
                            summary.getValidClaims(), summary.getInvalidClaims());
                    log.info("Total Amount: ${}", summary.getTotalAmount());
                    log.info("By Type: {}", summary.getClaimsByType());
                    log.info("By Status: {}", summary.getClaimsByStatus());
                    log.info("Flagged for Review: {}", summary.getFlaggedForReview());
                    log.info("════════════════════════════════════════════════════════════════");

                    return summary;
                })
                .get();
    }
}
