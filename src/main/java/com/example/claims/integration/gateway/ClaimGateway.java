package com.example.claims.integration.gateway;

import com.example.claims.model.Claim;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Claim Gateway Interface
 *
 * PATTERN: Messaging Gateway
 * PURPOSE: Provide a clean, domain-focused API that hides messaging complexity
 *
 * What it does:
 * - Your business code calls gateway.submitClaim(claim)
 * - Spring Integration handles: create message, set headers, send to channel
 * - Caller doesn't know about channels, messages, or integration
 *
 * Why this matters:
 * - Testable: Mock the gateway in unit tests
 * - Decoupled: Change messaging implementation without changing business code
 * - Clean: Service layer stays focused on business logic
 *
 * Interview tip: "Gateway hides the messaging infrastructure from business code.
 * It converts method calls to messages and vice versa."
 */
@MessagingGateway(
        name = "claimGateway",
        defaultRequestChannel = "claimInputChannel"
)
public interface ClaimGateway {

    /**
     * Submit a claim for processing.
     *
     * This method:
     * 1. Creates a Message with the Claim as payload
     * 2. Adds the source as a header
     * 3. Sends to claimInputChannel
     * 4. Returns the processed claim (after flowing through the pipeline)
     *
     * @param claim  The claim to process
     * @param source Where the claim came from (JSON, XML, CSV)
     * @return The processed claim with updated status
     */
    @Gateway(requestChannel = "claimInputChannel", replyChannel = "processedClaimsChannel")
    Claim submitClaim(Claim claim, @Header("source") String source);

    /**
     * Submit a claim without waiting for response (fire-and-forget).
     *
     * Use this for:
     * - Batch processing where you don't need immediate confirmation
     * - High-volume scenarios where latency matters
     * - Async workflows where processing happens later
     */
    @Gateway(requestChannel = "claimInputChannel")
    void submitClaimAsync(Claim claim, @Header("source") String source);
}
