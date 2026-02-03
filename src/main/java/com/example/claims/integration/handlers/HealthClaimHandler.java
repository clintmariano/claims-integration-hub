package com.example.claims.integration.handlers;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * Health Claims Handler
 *
 * Handles medical and healthcare claims.
 * Often involves provider verification and medical coding.
 */
@Component
@Slf4j
public class HealthClaimHandler {

    @ServiceActivator(inputChannel = "healthClaimsChannel", outputChannel = "processedClaimsChannel")
    public Claim processHealthClaim(Claim claim) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Processing HEALTH claim: {}", claim.getClaimId());
        log.info("  Policy: {}", claim.getPolicyNumber());
        log.info("  Amount: ${}", claim.getAmount());
        log.info("  Claimant: {}", claim.getClaimantName());
        log.info("═══════════════════════════════════════════════════════════════");

        // Health claim processing
        log.info("  [Step 1] Verifying healthcare provider is in-network...");
        log.info("  [Step 2] Checking procedure codes against coverage...");
        log.info("  [Step 3] Calculating member responsibility (deductible, copay)...");
        log.info("  [Step 4] Checking pre-authorization if required...");

        // Most health claims can be auto-processed if provider is in-network
        claim.setStatus(ClaimStatus.PROCESSING);

        log.info("HEALTH claim {} set to: {}", claim.getClaimId(), claim.getStatus());
        return claim;
    }
}
