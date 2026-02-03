package com.example.claims.integration.transformers;

import com.example.claims.dto.ClaimRequest;
import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Claim Transformer
 *
 * PATTERN: Transformer
 * PURPOSE: Convert a message payload from one format to another
 *
 * What it does:
 * - Receives ClaimRequest (external DTO)
 * - Returns Claim (internal domain model)
 * - Enriches with metadata (timestamps, IDs)
 *
 * Real-world use cases:
 * - JSON → internal object
 * - XML → JSON
 * - Legacy format → new format
 * - Enrich with data from other services
 *
 * Interview tip: "Transformer modifies the message payload without changing
 * the message flow. It's pure data transformation, not routing."
 */
@Component
@Slf4j
public class ClaimTransformer {

    /**
     * Transform/enrich incoming Claim object.
     *
     * The controller already converts DTO → Claim.
     * This transformer enriches with additional metadata and validation.
     *
     * This is where we:
     * 1. Ensure all IDs are generated
     * 2. Set timestamps if missing
     * 3. Add source information for traceability
     * 4. Flag high-value claims
     *
     * @param claim  The claim to enrich
     * @param source Header indicating the source (JSON, XML, CSV)
     * @return Enriched Claim domain object
     */
    @Transformer(inputChannel = "claimInputChannel", outputChannel = "validatedClaimsChannel")
    public Claim transformAndEnrich(Claim claim, @Header("source") String source) {
        log.info("Enriching claim from {} for policy: {}", source, claim.getPolicyNumber());

        // Ensure IDs are set
        if (claim.getClaimId() == null) {
            claim.setClaimId(UUID.randomUUID().toString());
        }
        if (claim.getCorrelationId() == null) {
            claim.setCorrelationId(UUID.randomUUID().toString());
        }

        // Set timestamps
        if (claim.getReceivedAt() == null) {
            claim.setReceivedAt(Instant.now());
        }

        // Set source
        claim.setSource(source);

        // Set initial status if not set
        if (claim.getStatus() == null) {
            claim.setStatus(ClaimStatus.SUBMITTED);
        }

        // Flag high-value claims
        claim.setFlaggedForReview(claim.isHighValue());

        log.info("Enriched claim {} - Type: {}, Amount: ${}, High-Value: {}",
                claim.getClaimId(),
                claim.getClaimType(),
                claim.getAmount(),
                claim.isFlaggedForReview());

        return claim;
    }

    /**
     * Transform a Claim directly (for internal use).
     * Adds tracking metadata if not present.
     */
    public Claim enrichClaim(Claim claim) {
        if (claim.getClaimId() == null) {
            claim.setClaimId(UUID.randomUUID().toString());
        }
        if (claim.getCorrelationId() == null) {
            claim.setCorrelationId(UUID.randomUUID().toString());
        }
        if (claim.getReceivedAt() == null) {
            claim.setReceivedAt(Instant.now());
        }
        if (claim.getStatus() == null) {
            claim.setStatus(ClaimStatus.SUBMITTED);
        }

        // Flag for review if high value
        claim.setFlaggedForReview(claim.isHighValue());

        return claim;
    }
}
