package com.example.claims.model;

/**
 * Status of a claim as it moves through the processing pipeline.
 *
 * This represents the claim lifecycle:
 * SUBMITTED → VALIDATED → PROCESSING → (APPROVED | DENIED) → PAID (if approved)
 *
 * In Spring Integration terms:
 * - SUBMITTED: Received at Gateway
 * - VALIDATED: Passed through Filter
 * - PROCESSING: Being handled by Service Activator
 * - APPROVED/DENIED: Final decision made
 * - PAID: Payment processed (via Service Bus queue)
 */
public enum ClaimStatus {
    SUBMITTED("Submitted", "Claim received, awaiting validation"),
    VALIDATED("Validated", "Claim passed validation checks"),
    PROCESSING("Processing", "Claim is being processed"),
    PENDING_REVIEW("Pending Review", "Claim requires manual review"),
    APPROVED("Approved", "Claim approved for payment"),
    DENIED("Denied", "Claim denied"),
    PAID("Paid", "Payment processed"),
    INVALID("Invalid", "Claim failed validation");

    private final String displayName;
    private final String description;

    ClaimStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
