package com.example.claims.integration.filters;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.messaging.handler.annotation.Header;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Claim Validation Filter
 *
 * PATTERN: Filter
 * PURPOSE: Pass or block messages based on criteria
 *
 * What it does:
 * - Evaluates each claim against validation rules
 * - Returns true: claim passes to next component
 * - Returns false: claim is discarded OR sent to discard channel
 *
 * Key concept: Filter makes a YES/NO decision.
 * It doesn't change the message or route to multiple destinations.
 * That's what Router does.
 *
 * Interview tip: "Filter is for binary decisions - should this message
 * continue or not? Use Router when you need multiple destinations."
 *
 * Configuration options:
 * - discardChannel: Where rejected messages go (instead of being dropped)
 * - throwExceptionOnRejection: Throw exception instead of discarding
 */
@MessageEndpoint
@Slf4j
public class ClaimValidationFilter {

    /**
     * Validate a claim before processing.
     *
     * Business rules:
     * 1. Policy number must be present
     * 2. Amount must be positive
     * 3. Incident date cannot be in the future
     * 4. Claim must have a type
     *
     * @param claim The claim to validate
     * @return true if valid, false if invalid
     */
    @Filter(
            inputChannel = "validatedClaimsChannel",
            outputChannel = "routingChannel",
            discardChannel = "invalidClaimsChannel"  // Don't lose invalid claims!
    )
    public boolean isValidClaim(Claim claim) {
        log.info("Validating claim: {}", claim.getClaimId());

        // Rule 1: Policy number required
        if (claim.getPolicyNumber() == null || claim.getPolicyNumber().isBlank()) {
            log.warn("Claim {} rejected: Missing policy number", claim.getClaimId());
            claim.setStatus(ClaimStatus.INVALID);
            return false;
        }

        // Rule 2: Amount must be positive
        if (claim.getAmount() == null || claim.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Claim {} rejected: Invalid amount {}", claim.getClaimId(), claim.getAmount());
            claim.setStatus(ClaimStatus.INVALID);
            return false;
        }

        // Rule 3: Incident date cannot be in the future
        if (claim.getIncidentDate() != null && claim.getIncidentDate().isAfter(LocalDate.now())) {
            log.warn("Claim {} rejected: Future incident date {}",
                    claim.getClaimId(), claim.getIncidentDate());
            claim.setStatus(ClaimStatus.INVALID);
            return false;
        }

        // Rule 4: Claim type required
        if (claim.getClaimType() == null) {
            log.warn("Claim {} rejected: Missing claim type", claim.getClaimId());
            claim.setStatus(ClaimStatus.INVALID);
            return false;
        }

        // All rules passed
        log.info("Claim {} passed validation", claim.getClaimId());
        claim.setStatus(ClaimStatus.VALIDATED);
        return true;
    }

    /**
     * Additional filter for high-value claims.
     * These need extra scrutiny before approval.
     *
     * This shows you can have multiple filters in a flow,
     * each checking different criteria.
     */
    public boolean requiresManualReview(Claim claim, @Header("source") String source) {
        // High-value claims always need review
        if (claim.isHighValue()) {
            log.info("Claim {} flagged for review: High value (${}))",
                    claim.getClaimId(), claim.getAmount());
            return true;
        }

        // Claims from batch files need review (less trusted source)
        if ("CSV".equals(source)) {
            log.info("Claim {} flagged for review: Batch source", claim.getClaimId());
            return true;
        }

        return false;
    }
}
