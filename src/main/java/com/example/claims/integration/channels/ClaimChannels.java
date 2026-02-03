package com.example.claims.integration.channels;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;

/**
 * Message Channel Definitions
 *
 * PATTERN: Message Channel
 * PURPOSE: Decouple message producers from consumers
 *
 * Think of channels as pipes:
 * - Producers write to one end
 * - Consumers read from the other
 * - Neither knows about the other
 *
 * Types of channels:
 * - DirectChannel: Synchronous, single consumer, fast
 * - QueueChannel: Asynchronous, buffers messages, can poll
 * - PublishSubscribeChannel: Multiple consumers get same message
 *
 * Interview tip: Know when to use each type!
 */
@Configuration
public class ClaimChannels {

    // ═══════════════════════════════════════════════════════════════════════
    // INPUT CHANNELS - Where claims enter the system
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Main entry point for all claims.
     * Gateway writes here, flow reads from here.
     *
     * Using DirectChannel because:
     * - We want synchronous processing for immediate response
     * - Only one consumer (the main flow) reads from this
     */
    @Bean
    public MessageChannel claimInputChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("claimInputChannel");
        return channel;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROCESSING CHANNELS - Internal routing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Channel for validated claims (passed the filter).
     */
    @Bean
    public MessageChannel validatedClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("validatedClaimsChannel");
        return channel;
    }

    /**
     * Channel for invalid claims (failed validation).
     * These go to error handling, not the main flow.
     */
    @Bean
    public MessageChannel invalidClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("invalidClaimsChannel");
        return channel;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROUTED CHANNELS - One per claim type
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Auto insurance claims channel.
     * Processed by AutoClaimHandler.
     */
    @Bean
    public MessageChannel autoClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("autoClaimsChannel");
        return channel;
    }

    /**
     * Property insurance claims channel.
     * Processed by PropertyClaimHandler.
     */
    @Bean
    public MessageChannel propertyClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("propertyClaimsChannel");
        return channel;
    }

    /**
     * Health insurance claims channel.
     * Processed by HealthClaimHandler.
     */
    @Bean
    public MessageChannel healthClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("healthClaimsChannel");
        return channel;
    }

    /**
     * High-value claims channel (amount > $10,000).
     * These require fraud review before processing.
     */
    @Bean
    public MessageChannel highValueClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("highValueClaimsChannel");
        return channel;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OUTPUT CHANNELS - Where processed claims go
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Channel for claims ready for payment processing.
     * In Phase 3, this connects to Azure Service Bus.
     */
    @Bean
    public MessageChannel paymentChannel() {
        // Using QueueChannel to buffer messages
        // This allows async processing and handles backpressure
        return new QueueChannel(100);
    }

    /**
     * Channel for audit events.
     * In Phase 3, this connects to Azure Event Hubs.
     */
    @Bean
    public MessageChannel auditChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("auditChannel");
        return channel;
    }

    /**
     * Channel for processed claims (end of flow).
     */
    @Bean
    public MessageChannel processedClaimsChannel() {
        DirectChannel channel = new DirectChannel();
        channel.setComponentName("processedClaimsChannel");
        return channel;
    }
}
