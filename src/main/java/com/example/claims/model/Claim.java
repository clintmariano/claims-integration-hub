package com.example.claims.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Core domain model for an insurance claim.
 *
 * This is the INTERNAL representation used throughout the integration flow.
 * External systems may send JSON, XML, or CSV - all get transformed into this.
 *
 * Key fields for routing decisions:
 * - claimType: Determines which processing channel (AUTO, PROPERTY, HEALTH)
 * - amount: High-value claims (>$10,000) get flagged for fraud review
 * - status: Tracks progression through the pipeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    /**
     * Unique identifier for this claim.
     * Generated on creation, used for correlation in Aggregator patterns.
     */
    private String claimId;

    /**
     * Policy number from the insurance policy.
     * Used to validate the claim against policy database.
     */
    private String policyNumber;

    /**
     * Type of claim - determines routing path.
     * This is the key field for the Router pattern.
     */
    private ClaimType claimType;

    /**
     * Current status in the processing pipeline.
     * Updated by each component in the flow.
     */
    private ClaimStatus status;

    /**
     * Claimed amount in dollars.
     * High-value claims (>$10,000) trigger additional review.
     */
    private BigDecimal amount;

    /**
     * Description of the incident/claim.
     */
    private String description;

    /**
     * Date when the incident occurred.
     */
    private LocalDate incidentDate;

    /**
     * Date when claim was submitted.
     */
    private LocalDate submissionDate;

    /**
     * Name of the claimant (policyholder).
     */
    private String claimantName;

    /**
     * Email for notifications.
     */
    private String claimantEmail;

    // Metadata for tracking through the integration flow

    /**
     * When this claim entered the system.
     * Used for SLA tracking and metrics.
     */
    private Instant receivedAt;

    /**
     * Source system/format (JSON, XML, CSV).
     * Useful for debugging and analytics.
     */
    private String source;

    /**
     * Correlation ID for tracking across systems.
     * Essential for distributed tracing.
     */
    private String correlationId;

    /**
     * Whether this claim was flagged for fraud review.
     * Set by the Filter when amount > threshold.
     */
    private boolean flaggedForReview;

    /**
     * Creates a new claim with generated ID and timestamps.
     */
    public static Claim createNew() {
        return Claim.builder()
                .claimId(UUID.randomUUID().toString())
                .status(ClaimStatus.SUBMITTED)
                .receivedAt(Instant.now())
                .submissionDate(LocalDate.now())
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Check if this is a high-value claim requiring additional review.
     * Used by Filter and Router to flag suspicious claims.
     */
    public boolean isHighValue() {
        return amount != null && amount.compareTo(new BigDecimal("10000")) > 0;
    }
}
