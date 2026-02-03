package com.example.claims.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Summary of a batch processing run.
 *
 * Created by the Aggregator after all claims in a batch are processed.
 * This is the OUTPUT of aggregation - multiple claims combined into one summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSummary {

    /**
     * Unique ID for this batch.
     */
    private String batchId;

    /**
     * When the batch started processing.
     */
    private Instant startTime;

    /**
     * When the batch finished processing.
     */
    private Instant endTime;

    /**
     * Total number of claims in the batch.
     */
    private int totalClaims;

    /**
     * Number of claims that passed validation.
     */
    private int validClaims;

    /**
     * Number of claims that failed validation.
     */
    private int invalidClaims;

    /**
     * Breakdown by claim type.
     * Key: ClaimType name, Value: count
     */
    @Builder.Default
    private Map<String, Integer> claimsByType = new HashMap<>();

    /**
     * Breakdown by status after processing.
     * Key: ClaimStatus name, Value: count
     */
    @Builder.Default
    private Map<String, Integer> claimsByStatus = new HashMap<>();

    /**
     * Total dollar amount of all claims.
     */
    private BigDecimal totalAmount;

    /**
     * Number of claims flagged for review.
     */
    private int flaggedForReview;

    /**
     * Add a count to a type.
     */
    public void incrementType(String type) {
        claimsByType.merge(type, 1, Integer::sum);
    }

    /**
     * Add a count to a status.
     */
    public void incrementStatus(String status) {
        claimsByStatus.merge(status, 1, Integer::sum);
    }
}
