package com.example.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

/**
 * Claims Integration Hub - Spring Integration Learning Project
 *
 * This application demonstrates Enterprise Integration Patterns (EIP):
 * - Gateway: Clean API interface for submitting claims
 * - Transformer: Convert between data formats (JSON/XML/CSV → Claim)
 * - Filter: Validate claims before processing
 * - Router: Route claims by type (AUTO, PROPERTY, HEALTH)
 * - Splitter: Break batch files into individual claims
 * - Aggregator: Combine claims for reporting
 * - Service Activator: Process claims in handlers
 *
 * Azure Integration:
 * - Service Bus: Reliable message queuing for payments
 * - Event Hubs: Event streaming for audit trail
 */
@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
public class ClaimsIntegrationHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsIntegrationHubApplication.class, args);
    }
}
