package com.example.claims.integration.handlers;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * High-Value Claims Handler
 *
 * Special handling for claims over $10,000.
 * These require fraud screening and senior adjuster review.
 *
 * This demonstrates how the Router pattern enables
 * specialized processing paths based on content.
 */
@Component
@Slf4j
public class HighValueClaimHandler {

    @ServiceActivator(inputChannel = "highValueClaimsChannel", outputChannel = "processedClaimsChannel")
    public Claim processHighValueClaim(Claim claim) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("⚠️  HIGH-VALUE CLAIM DETECTED: {}", claim.getClaimId());
        log.info("  Type: {}", claim.getClaimType());
        log.info("  Amount: ${} (exceeds $10,000 threshold)", claim.getAmount());
        log.info("  Policy: {}", claim.getPolicyNumber());
        log.info("  Claimant: {}", claim.getClaimantName());
        log.info("═══════════════════════════════════════════════════════════════");

        // High-value claims get extra scrutiny
        log.info("  [Step 1] Running fraud detection algorithms...");
        log.info("  [Step 2] Checking claimant history across all policies...");
        log.info("  [Step 3] Verifying incident with third-party sources...");
        log.info("  [Step 4] Flagging for senior adjuster assignment...");

        // High-value claims always require manual review
        claim.setStatus(ClaimStatus.PENDING_REVIEW);
        claim.setFlaggedForReview(true);

        log.info("⚠️  HIGH-VALUE claim {} flagged for review", claim.getClaimId());
        return claim;
    }
}
