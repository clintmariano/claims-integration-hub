package com.example.claims.integration.handlers;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Auto Claims Handler
 *
 * PATTERN: Service Activator
 * PURPOSE: Invoke business logic when a message arrives
 *
 * What it does:
 * - Receives claims routed to autoClaimsChannel
 * - Performs auto-specific processing logic
 * - Returns the processed claim (or void for fire-and-forget)
 *
 * This is where your ACTUAL BUSINESS LOGIC lives.
 * The integration patterns (Gateway, Filter, Router) get the message here.
 * The Service Activator does the real work.
 *
 * Interview tip: "Service Activator is the bridge between messaging and
 * business logic. It's where I put the code that actually processes
 * the message - calling services, updating databases, etc."
 */
@Component
@Slf4j
public class AutoClaimHandler {

    /**
     * Process an auto insurance claim.
     *
     * In a real system, this would:
     * - Validate against policy database
     * - Check coverage limits
     * - Request vehicle inspection if needed
     * - Calculate deductible
     * - Approve/deny based on policy terms
     *
     * @param claim The auto insurance claim
     * @return Processed claim with updated status
     */
    @ServiceActivator(inputChannel = "autoClaimsChannel", outputChannel = "processedClaimsChannel")
    public Claim processAutoClaim(Claim claim, @Header("source") String source) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Processing AUTO claim: {}", claim.getClaimId());
        log.info("  Policy: {}", claim.getPolicyNumber());
        log.info("  Amount: ${}", claim.getAmount());
        log.info("  Incident Date: {}", claim.getIncidentDate());
        log.info("  Source: {}", source);
        log.info("═══════════════════════════════════════════════════════════════");

        // Simulate processing logic
        try {
            // Step 1: Verify policy is active
            log.info("  [Step 1] Verifying policy {} is active...", claim.getPolicyNumber());
            Thread.sleep(100); // Simulate database call

            // Step 2: Check coverage
            log.info("  [Step 2] Checking coverage limits...");
            Thread.sleep(100);

            // Step 3: Auto-approve small claims
            if (claim.getAmount().compareTo(new java.math.BigDecimal("1000")) < 0) {
                log.info("  [Step 3] Small claim - auto-approved");
                claim.setStatus(ClaimStatus.APPROVED);
            } else {
                log.info("  [Step 3] Claim requires adjuster review");
                claim.setStatus(ClaimStatus.PENDING_REVIEW);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            claim.setStatus(ClaimStatus.PROCESSING);
        }

        log.info("AUTO claim {} processed with status: {}", claim.getClaimId(), claim.getStatus());
        return claim;
    }
}
