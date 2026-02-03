package com.example.claims.integration.flows;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

/**
 * Claim Intake Integration Flow
 *
 * This class demonstrates the IntegrationFlow DSL approach.
 * Instead of annotations on methods, we define flows programmatically.
 *
 * Benefits of DSL approach:
 * - Flows are defined in one place (easier to understand)
 * - More flexible composition
 * - Better for complex flows
 * - Can be conditionally created
 *
 * This flow handles general claims and the routing channel.
 */
@Configuration
@Slf4j
public class ClaimIntakeFlow {

    /**
     * Channel for routing decisions.
     * Claims come here after validation, before routing by type.
     */
    @Bean
    public MessageChannel routingChannel() {
        return new DirectChannel();
    }

    /**
     * Channel for general claims (LIFE, LIABILITY).
     */
    @Bean
    public MessageChannel generalClaimsChannel() {
        return new DirectChannel();
    }

    /**
     * Flow for handling general claims (LIFE, LIABILITY).
     *
     * This shows the DSL approach to creating a flow:
     * - IntegrationFlow.from() - specify input channel
     * - .handle() - process the message
     * - .channel() - send to output channel
     */
    @Bean
    public IntegrationFlow generalClaimsFlow() {
        return IntegrationFlow
                .from("generalClaimsChannel")
                .handle(Claim.class, (claim, headers) -> {
                    log.info("═══════════════════════════════════════════════════════════════");
                    log.info("Processing GENERAL claim: {}", claim.getClaimId());
                    log.info("  Type: {} ({})", claim.getClaimType(), claim.getClaimType().getDisplayName());
                    log.info("  Amount: ${}", claim.getAmount());
                    log.info("═══════════════════════════════════════════════════════════════");

                    claim.setStatus(ClaimStatus.PROCESSING);
                    return claim;
                })
                .channel("processedClaimsChannel")
                .get();
    }

    /**
     * Flow that handles all processed claims.
     *
     * This flow:
     * 1. Logs the completed claim
     * 2. Sends to auditChannel (→ Event Hubs for audit trail)
     * 3. Sends to paymentChannel (→ Service Bus for payment if approved)
     *
     * Uses publishSubscribe to send the same message to multiple channels.
     */
    @Bean
    public IntegrationFlow processedClaimsLoggingFlow() {
        return IntegrationFlow
                .from("processedClaimsChannel")
                .handle(Claim.class, (claim, headers) -> {
                    log.info("════════════════════════════════════════════════════════════════");
                    log.info("✅ CLAIM PROCESSING COMPLETE");
                    log.info("  Claim ID: {}", claim.getClaimId());
                    log.info("  Type: {}", claim.getClaimType());
                    log.info("  Status: {}", claim.getStatus());
                    log.info("  Amount: ${}", claim.getAmount());
                    log.info("  Correlation ID: {}", claim.getCorrelationId());
                    log.info("════════════════════════════════════════════════════════════════");
                    return claim;
                })
                // Send to BOTH audit and payment channels
                .publishSubscribeChannel(pubSub -> pubSub
                        .subscribe(flow -> flow.channel("auditChannel"))
                        .subscribe(flow -> flow.channel("paymentChannel"))
                )
                .get();
    }

    /**
     * Alternative: Full flow defined with DSL (for reference).
     *
     * This shows how you COULD define the entire flow in one place
     * instead of using annotations. Both approaches are valid.
     *
     * Uncomment to see how it works:
     *
     * @Bean
     * public IntegrationFlow fullClaimProcessingFlow() {
     *     return IntegrationFlow
     *             .from("claimInputChannel")
     *             // Transform
     *             .transform(ClaimRequest.class, request -> {
     *                 // transform logic here
     *                 return new Claim();
     *             })
     *             // Filter
     *             .filter(Claim.class, claim -> claim.getPolicyNumber() != null)
     *             // Route
     *             .<Claim, String>route(
     *                 claim -> claim.getClaimType().name(),
     *                 mapping -> mapping
     *                     .channelMapping("AUTO", "autoClaimsChannel")
     *                     .channelMapping("PROPERTY", "propertyClaimsChannel")
     *                     .channelMapping("HEALTH", "healthClaimsChannel")
     *             )
     *             .get();
     * }
     */
}
