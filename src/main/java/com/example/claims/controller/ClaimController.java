package com.example.claims.controller;

import com.example.claims.dto.BatchSummary;
import com.example.claims.dto.ClaimRequest;
import com.example.claims.dto.ClaimResponse;
import com.example.claims.integration.gateway.BatchClaimGateway;
import com.example.claims.integration.gateway.ClaimGateway;
import com.example.claims.integration.transformers.ClaimTransformer;
import com.example.claims.model.Claim;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * REST Controller for Claims
 *
 * This controller:
 * 1. Receives HTTP requests
 * 2. Transforms request to domain model
 * 3. Sends to Gateway (entry point to Spring Integration)
 * 4. Returns response
 *
 * The controller is THIN - it doesn't contain business logic.
 * All processing happens in the integration flow.
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimGateway claimGateway;
    private final BatchClaimGateway batchClaimGateway;
    private final ClaimTransformer claimTransformer;

    /**
     * Submit a new claim.
     *
     * Flow:
     * 1. Validate request (via @Valid)
     * 2. Transform to Claim domain object
     * 3. Send through Gateway to integration flow
     * 4. Return response with claim ID for tracking
     *
     * POST /api/claims
     * Content-Type: application/json
     */
    @PostMapping
    public ResponseEntity<ClaimResponse> submitClaim(@Valid @RequestBody ClaimRequest request) {
        log.info("Received claim submission for policy: {}", request.getPolicyNumber());

        // Transform request to domain model
        Claim claim = Claim.builder()
                .policyNumber(request.getPolicyNumber())
                .claimType(request.getClaimType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .incidentDate(request.getIncidentDate())
                .claimantName(request.getClaimantName())
                .claimantEmail(request.getClaimantEmail())
                .build();

        // Enrich with metadata
        claim = claimTransformer.enrichClaim(claim);

        try {
            // Send through the integration gateway
            // This enters the Spring Integration flow
            Claim processedClaim = claimGateway.submitClaim(claim, "JSON");

            log.info("Claim {} processed with status: {}",
                    processedClaim.getClaimId(), processedClaim.getStatus());

            return ResponseEntity.ok(ClaimResponse.from(
                    processedClaim,
                    "Claim submitted successfully and is being processed"
            ));

        } catch (Exception e) {
            log.error("Error processing claim: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ClaimResponse.builder()
                            .claimId(claim.getClaimId())
                            .message("Error processing claim: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Submit a claim asynchronously (fire-and-forget).
     *
     * Use this for high-volume scenarios where you don't need
     * to wait for processing to complete.
     *
     * POST /api/claims/async
     */
    @PostMapping("/async")
    public ResponseEntity<ClaimResponse> submitClaimAsync(@Valid @RequestBody ClaimRequest request) {
        log.info("Received async claim submission for policy: {}", request.getPolicyNumber());

        Claim claim = Claim.builder()
                .policyNumber(request.getPolicyNumber())
                .claimType(request.getClaimType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .incidentDate(request.getIncidentDate())
                .claimantName(request.getClaimantName())
                .claimantEmail(request.getClaimantEmail())
                .build();

        claim = claimTransformer.enrichClaim(claim);

        // Fire and forget - don't wait for processing
        claimGateway.submitClaimAsync(claim, "JSON");

        return ResponseEntity.accepted()
                .body(ClaimResponse.from(claim,
                        "Claim accepted for processing. Track with ID: " + claim.getClaimId()));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Claims Integration Hub is running");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH PROCESSING ENDPOINTS (Phase 2: Splitter + Aggregator)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Submit a batch of claims via CSV file.
     *
     * Flow:
     * 1. Receive CSV file
     * 2. Send to Splitter (breaks into individual claims)
     * 3. Each claim processed independently
     * 4. Aggregator combines results
     * 5. Return BatchSummary
     *
     * POST /api/claims/batch
     * Content-Type: multipart/form-data
     *
     * Example CSV:
     * policyNumber,claimType,amount,description,incidentDate,claimantName,claimantEmail
     * POL-001,AUTO,2500.00,Fender bender,2024-01-15,John Doe,john@email.com
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchSummary> submitBatch(@RequestParam("file") MultipartFile file) {
        log.info("Received batch file: {}", file.getOriginalFilename());

        try {
            String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String batchId = UUID.randomUUID().toString();

            log.info("Processing batch {} with {} bytes", batchId, csvContent.length());

            // Send through batch gateway
            // This triggers: Splitter → Process each → Aggregator → Summary
            BatchSummary summary = batchClaimGateway.submitBatch(csvContent, batchId);

            return ResponseEntity.ok(summary);

        } catch (IOException e) {
            log.error("Error reading batch file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    BatchSummary.builder()
                            .batchId("ERROR")
                            .totalClaims(0)
                            .build()
            );
        }
    }

    /**
     * Submit batch via raw CSV content (for testing without file upload).
     *
     * POST /api/claims/batch/raw
     * Content-Type: text/plain
     */
    @PostMapping(value = "/batch/raw", consumes = "text/plain")
    public ResponseEntity<BatchSummary> submitBatchRaw(@RequestBody String csvContent) {
        log.info("Received raw batch content ({} chars)", csvContent.length());

        String batchId = UUID.randomUUID().toString();
        BatchSummary summary = batchClaimGateway.submitBatch(csvContent, batchId);

        return ResponseEntity.ok(summary);
    }
}
