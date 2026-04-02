package com.orthocarechain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PrescriptionRequest {

    @NotBlank(message = "Drug name is required")
    @Size(max = 100, message = "Drug name must not exceed 100 characters")
    private String drugName;

    @NotBlank(message = "Dosage is required")
    @Size(max = 50, message = "Dosage must not exceed 50 characters")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    @Size(max = 50, message = "Frequency must not exceed 50 characters")
    private String frequency;

    @NotBlank(message = "Duration is required")
    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;

    @Size(max = 500, message = "Instructions must not exceed 500 characters")
    private String instructions;

    private LocalDate medicationStartDate;

    private LocalDate medicationEndDate;

    @Min(value = 0, message = "Refills allowed cannot be negative")
    private Integer refillsAllowed;

    @Size(max = 500, message = "Side effects must not exceed 500 characters")
    private String sideEffects;

    @Size(max = 500, message = "Contraindications must not exceed 500 characters")
    private String contraindications;
}
