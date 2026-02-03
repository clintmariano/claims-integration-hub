package com.example.claims.integration.handlers;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * Property Claims Handler
 *
 * Handles home and property damage claims.
 * Different processing logic than auto claims.
 */
@Component
@Slf4j
public class PropertyClaimHandler {

    @ServiceActivator(inputChannel = "propertyClaimsChannel", outputChannel = "processedClaimsChannel")
    public Claim processPropertyClaim(Claim claim) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Processing PROPERTY claim: {}", claim.getClaimId());
        log.info("  Policy: {}", claim.getPolicyNumber());
        log.info("  Amount: ${}", claim.getAmount());
        log.info("  Description: {}", claim.getDescription());
        log.info("═══════════════════════════════════════════════════════════════");

        // Property claims typically require inspection
        log.info("  [Step 1] Scheduling property inspection...");
        log.info("  [Step 2] Checking for prior claims on property...");
        log.info("  [Step 3] Verifying coverage type (replacement vs actual value)...");

        // Property claims usually need review due to inspection requirement
        claim.setStatus(ClaimStatus.PENDING_REVIEW);

        log.info("PROPERTY claim {} set to: {}", claim.getClaimId(), claim.getStatus());
        return claim;
    }
}
