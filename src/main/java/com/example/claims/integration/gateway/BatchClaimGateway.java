package com.example.claims.integration.gateway;

import com.example.claims.dto.BatchSummary;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Gateway for batch claim processing.
 *
 * Separate from single-claim gateway for clarity.
 * Demonstrates how you can have multiple gateways for different use cases.
 */
@MessagingGateway(name = "batchClaimGateway")
public interface BatchClaimGateway {

    /**
     * Submit a batch of claims (CSV format).
     *
     * Flow:
     * 1. CSV content sent to batchInputChannel
     * 2. Splitter breaks into individual claims
     * 3. Each claim processed
     * 4. Aggregator combines results
     * 5. Returns BatchSummary
     *
     * @param csvContent The CSV file content
     * @param batchId    Unique identifier for this batch
     * @return Summary of the batch processing
     */
    @Gateway(requestChannel = "batchInputChannel", replyChannel = "batchSummaryChannel")
    BatchSummary submitBatch(String csvContent, @Header("batchId") String batchId);

    /**
     * Submit batch asynchronously (fire-and-forget).
     */
    @Gateway(requestChannel = "batchInputChannel")
    void submitBatchAsync(String csvContent, @Header("batchId") String batchId);
}
