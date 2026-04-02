package com.orthocarechain.dto.response;

import com.orthocarechain.enums.SeverityLevel;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDate visitDate;
    private LocalDate nextVisitDate;
    private String diagnosis;
    private String diagnosisDetails;
    private SeverityLevel severityLevel;
    private String doctorNotes;
    private String treatmentPlan;
    private String followUpInstructions;
    private Boolean isActive;
    private List<PrescriptionResponse> prescriptions;
    private List<MedicalImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
