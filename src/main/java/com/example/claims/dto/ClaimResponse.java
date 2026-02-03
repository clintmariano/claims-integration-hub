package com.example.claims.dto;

import com.example.claims.model.Claim;
import com.example.claims.model.ClaimStatus;
import com.example.claims.model.ClaimType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for claim responses.
 *
 * Returns confirmation of claim submission with tracking information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {

    private String claimId;
    private String policyNumber;
    private ClaimType claimType;
    private ClaimStatus status;
    private BigDecimal amount;
    private Instant receivedAt;
    private String correlationId;
    private String message;

    /**
     * Create response from domain model.
     */
    public static ClaimResponse from(Claim claim, String message) {
        return ClaimResponse.builder()
                .claimId(claim.getClaimId())
                .policyNumber(claim.getPolicyNumber())
                .claimType(claim.getClaimType())
                .status(claim.getStatus())
                .amount(claim.getAmount())
                .receivedAt(claim.getReceivedAt())
                .correlationId(claim.getCorrelationId())
                .message(message)
                .build();
    }
}
