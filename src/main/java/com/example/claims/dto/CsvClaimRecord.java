package com.example.claims.dto;

import com.example.claims.model.ClaimType;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for CSV batch claim records.
 *
 * Used with OpenCSV to parse batch files.
 * Each row in the CSV becomes one CsvClaimRecord.
 *
 * Example CSV:
 * policyNumber,claimType,amount,description,incidentDate,claimantName,claimantEmail
 * POL-001,AUTO,2500.00,Fender bender,2024-01-15,John Doe,john@email.com
 * POL-002,HEALTH,500.00,Doctor visit,2024-01-16,Jane Smith,jane@email.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvClaimRecord {

    @CsvBindByName(column = "policyNumber", required = true)
    private String policyNumber;

    @CsvBindByName(column = "claimType", required = true)
    private String claimType;

    @CsvBindByName(column = "amount", required = true)
    private BigDecimal amount;

    @CsvBindByName(column = "description")
    private String description;

    @CsvBindByName(column = "incidentDate")
    @CsvDate("yyyy-MM-dd")
    private LocalDate incidentDate;

    @CsvBindByName(column = "claimantName", required = true)
    private String claimantName;

    @CsvBindByName(column = "claimantEmail", required = true)
    private String claimantEmail;

    /**
     * Parse claim type string to enum.
     */
    public ClaimType getClaimTypeEnum() {
        try {
            return ClaimType.valueOf(claimType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ClaimType.AUTO; // Default fallback
        }
    }
}
