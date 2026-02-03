package com.example.claims.dto;

import com.example.claims.model.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for incoming claim requests (JSON format).
 *
 * This is SEPARATE from the domain model (Claim) because:
 * 1. External API contract should not expose internal fields
 * 2. Validation rules differ for input vs internal processing
 * 3. We might accept different formats (JSON, XML, CSV) that all map to Claim
 *
 * The Transformer pattern converts this DTO → Claim domain object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRequest {

    @NotBlank(message = "Policy number is required")
    private String policyNumber;

    @NotNull(message = "Claim type is required")
    private ClaimType claimType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Incident date is required")
    private LocalDate incidentDate;

    @NotBlank(message = "Claimant name is required")
    private String claimantName;

    @NotBlank(message = "Claimant email is required")
    private String claimantEmail;
}
