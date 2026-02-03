package com.example.claims.model;

/**
 * Types of insurance claims.
 *
 * In a real system, each type would have different:
 * - Processing rules
 * - Required documentation
 * - Approval workflows
 * - Payment limits
 */
public enum ClaimType {
    AUTO("Auto Insurance", "Vehicle-related claims"),
    PROPERTY("Property Insurance", "Home and property damage claims"),
    HEALTH("Health Insurance", "Medical and healthcare claims"),
    LIFE("Life Insurance", "Life insurance claims"),
    LIABILITY("Liability Insurance", "Third-party liability claims");

    private final String displayName;
    private final String description;

    ClaimType(String displayName, String description) {
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
