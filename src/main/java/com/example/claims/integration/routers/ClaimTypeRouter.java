package com.example.claims.integration.routers;

import com.example.claims.model.Claim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;

/**
 * Claim Type Router
 *
 * PATTERN: Content-Based Router
 * PURPOSE: Route messages to different channels based on content
 *
 * What it does:
 * - Examines the claim type
 * - Returns the name of the channel to route to
 * - Each claim type goes to a specialized handler
 *
 * Key difference from Filter:
 * - Filter: YES/NO (binary decision)
 * - Router: WHICH ONE? (multiple destinations)
 *
 * Interview tip: "I use Router when a message needs to go to one of several
 * possible destinations based on its content. For example, routing claims
 * by type so each type can have specialized processing logic."
 *
 * Router types:
 * - Return String: channel name
 * - Return Collection<String>: multiple channels (message goes to all)
 * - Return MessageChannel: the actual channel object
 */
@MessageEndpoint
@Slf4j
public class ClaimTypeRouter {

    /**
     * Route claims to appropriate processing channel based on type.
     *
     * Flow:
     * - AUTO claims → autoClaimsChannel → AutoClaimHandler
     * - PROPERTY claims → propertyClaimsChannel → PropertyClaimHandler
     * - HEALTH claims → healthClaimsChannel → HealthClaimHandler
     * - HIGH VALUE claims → highValueClaimsChannel → Fraud review
     *
     * @param claim The claim to route
     * @return Channel name to route to
     */
    @Router(inputChannel = "routingChannel")
    public String routeByClaimType(Claim claim) {
        log.info("Routing claim {} of type {}", claim.getClaimId(), claim.getClaimType());

        // High-value claims go to special handling regardless of type
        if (claim.isHighValue()) {
            log.info("Claim {} routed to HIGH VALUE channel (amount: ${})",
                    claim.getClaimId(), claim.getAmount());
            return "highValueClaimsChannel";
        }

        // Route by claim type
        String channel = switch (claim.getClaimType()) {
            case AUTO -> "autoClaimsChannel";
            case PROPERTY -> "propertyClaimsChannel";
            case HEALTH -> "healthClaimsChannel";
            case LIFE, LIABILITY -> "generalClaimsChannel";
        };

        log.info("Claim {} routed to {} channel", claim.getClaimId(), channel);
        return channel;
    }

    /**
     * Alternative: Route to multiple channels.
     *
     * Use this when a message needs to go to multiple destinations.
     * For example: route to processing AND audit channel.
     *
     * Uncomment to see how it works:
     *
     * @Router(inputChannel = "routingChannel")
     * public List<String> routeToMultiple(Claim claim) {
     *     List<String> channels = new ArrayList<>();
     *
     *     // Always route to type-specific channel
     *     channels.add(routeByClaimType(claim));
     *
     *     // Also route to audit channel
     *     channels.add("auditChannel");
     *
     *     // High-value also goes to fraud detection
     *     if (claim.isHighValue()) {
     *         channels.add("fraudDetectionChannel");
     *     }
     *
     *     return channels;
     * }
     */
}
