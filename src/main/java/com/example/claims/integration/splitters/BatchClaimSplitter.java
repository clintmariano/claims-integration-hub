package com.example.claims.integration.splitters;

import com.example.claims.dto.CsvClaimRecord;
import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Splitter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Batch Claim Splitter
 *
 * PATTERN: Splitter
 * PURPOSE: Break one message into many messages
 *
 * What it does:
 * - Receives a CSV string (one message with many claims)
 * - Parses CSV into individual records
 * - Returns a Collection of Claims (each becomes a separate message)
 *
 * Real-world use cases:
 * - Batch file processing (CSV, fixed-width, etc.)
 * - Order with multiple line items
 * - Report with multiple records
 *
 * Interview tip: "Splitter takes one message and produces many.
 * Each item in the returned collection becomes a separate message
 * that flows independently through the rest of the pipeline."
 *
 * Key concept: Messages from a Splitter are CORRELATED.
 * They share a correlation ID so an Aggregator can recombine them later.
 */
@Component
@Slf4j
public class BatchClaimSplitter {

    /**
     * Split a CSV batch into individual claims.
     *
     * The @Splitter annotation tells Spring Integration:
     * 1. Read from batchInputChannel
     * 2. Call this method
     * 3. For each item in the returned Collection, create a message
     * 4. Send each message to batchProcessingChannel
     *
     * Spring automatically adds headers for correlation:
     * - correlationId: Same for all messages from this split
     * - sequenceNumber: Position in the original batch (1, 2, 3...)
     * - sequenceSize: Total number of items in the batch
     *
     * @param csvContent The raw CSV content
     * @param batchId    Batch identifier from header
     * @return Collection of Claims, each becomes a separate message
     */
    @Splitter(inputChannel = "batchInputChannel", outputChannel = "batchProcessingChannel")
    public Collection<Claim> splitBatch(String csvContent, @Header("batchId") String batchId) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("SPLITTER: Processing batch {}", batchId);
        log.info("═══════════════════════════════════════════════════════════════");

        // Parse CSV into records
        List<CsvClaimRecord> records = parseCsv(csvContent);
        log.info("Parsed {} records from CSV", records.size());

        // Convert each record to a Claim
        List<Claim> claims = records.stream()
                .map(record -> convertToClaim(record, batchId))
                .collect(Collectors.toList());

        log.info("SPLITTER: Splitting batch {} into {} individual claims", batchId, claims.size());

        // Each claim in this list becomes a SEPARATE MESSAGE
        // They will flow independently through the pipeline
        // But they're correlated - an Aggregator can recombine them later
        return claims;
    }

    /**
     * Parse CSV content into records.
     */
    private List<CsvClaimRecord> parseCsv(String csvContent) {
        try {
            return new CsvToBeanBuilder<CsvClaimRecord>(new StringReader(csvContent))
                    .withType(CsvClaimRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        } catch (Exception e) {
            log.error("Error parsing CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to parse CSV batch", e);
        }
    }

    /**
     * Convert a CSV record to a Claim domain object.
     */
    private Claim convertToClaim(CsvClaimRecord record, String batchId) {
        return Claim.builder()
                .claimId(UUID.randomUUID().toString())
                .correlationId(batchId) // All claims in batch share this
                .policyNumber(record.getPolicyNumber())
                .claimType(record.getClaimTypeEnum())
                .amount(record.getAmount())
                .description(record.getDescription())
                .incidentDate(record.getIncidentDate())
                .claimantName(record.getClaimantName())
                .claimantEmail(record.getClaimantEmail())
                .status(ClaimStatus.SUBMITTED)
                .receivedAt(Instant.now())
                .submissionDate(LocalDate.now())
                .source("CSV")
                .build();
    }
}
