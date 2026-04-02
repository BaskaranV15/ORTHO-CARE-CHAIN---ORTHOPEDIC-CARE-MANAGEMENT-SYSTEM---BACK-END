package com.orthocarechain.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {
    private Long id;
    private Long reportId;
    private String drugName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private LocalDate medicationStartDate;
    private LocalDate medicationEndDate;
    private Integer refillsAllowed;
    private Boolean isActive;
    private String sideEffects;
    private String contraindications;
    private LocalDateTime createdAt;
}
