package com.orthocarechain.dto.request;

import com.orthocarechain.enums.SeverityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReportRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Visit date is required")
    private LocalDate visitDate;

    private LocalDate nextVisitDate;

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 200, message = "Diagnosis must not exceed 200 characters")
    private String diagnosis;

    @Size(max = 5000, message = "Diagnosis details must not exceed 5000 characters")
    private String diagnosisDetails;

    @NotNull(message = "Severity level is required")
    private SeverityLevel severityLevel;

    @Size(max = 5000, message = "Doctor notes must not exceed 5000 characters")
    private String doctorNotes;

    @Size(max = 5000, message = "Treatment plan must not exceed 5000 characters")
    private String treatmentPlan;

    @Size(max = 2000, message = "Follow-up instructions must not exceed 2000 characters")
    private String followUpInstructions;

    @Valid
    private List<PrescriptionRequest> prescriptions;
}
