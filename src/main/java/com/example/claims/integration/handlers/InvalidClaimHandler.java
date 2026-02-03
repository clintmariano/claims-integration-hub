package com.example.claims.integration.handlers;

import com.example.claims.model.Claim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * Invalid Claims Handler
 *
 * Handles claims that failed validation.
 * Instead of losing them, we log and store for review.
 *
 * This is connected to the Filter's discardChannel.
 * It demonstrates that rejected messages don't have to be lost.
 *
 * Interview tip: "I always configure a discardChannel on filters
 * so rejected messages are captured for audit and debugging.
 * In production, you don't want to silently lose data."
 */
@Component
@Slf4j
public class InvalidClaimHandler {

    @ServiceActivator(inputChannel = "invalidClaimsChannel")
    public void handleInvalidClaim(Claim claim) {
        log.error("═══════════════════════════════════════════════════════════════");
        log.error("❌ INVALID CLAIM RECEIVED: {}", claim.getClaimId());
        log.error("  Status: {}", claim.getStatus());
        log.error("  Policy: {}", claim.getPolicyNumber());
        log.error("  Type: {}", claim.getClaimType());
        log.error("  Amount: {}", claim.getAmount());
        log.error("  Incident Date: {}", claim.getIncidentDate());
        log.error("═══════════════════════════════════════════════════════════════");

        // In a real system, we would:
        // 1. Store in a separate invalid_claims table
        // 2. Send notification to operations team
        // 3. Create a case for manual review
        // 4. Send rejection notice to claimant

        log.error("Invalid claim {} logged for review", claim.getClaimId());

        // Note: This handler doesn't return anything (void)
        // The message flow ends here - this is a terminal handler
    }
}
