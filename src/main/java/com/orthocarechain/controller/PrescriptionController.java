package com.orthocarechain.controller;

import com.orthocarechain.dto.request.PrescriptionRequest;
import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.PrescriptionResponse;
import com.orthocarechain.entity.Prescription;
import com.orthocarechain.entity.Report;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.exception.UnauthorizedAccessException;
import com.orthocarechain.repository.DoctorRepository;
import com.orthocarechain.repository.PrescriptionRepository;
import com.orthocarechain.repository.ReportRepository;
import com.orthocarechain.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final ReportRepository reportRepository;
    private final DoctorRepository doctorRepository;

    /**
     * POST /api/prescriptions/report/{reportId}
     * Requires: DOCTOR (own reports only) or ADMIN — adds prescription to existing report
     */
    @PostMapping("/report/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> addPrescription(
            @PathVariable Long reportId,
            @Valid @RequestBody PrescriptionRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        // Ownership check for doctors
        if ("ROLE_DOCTOR".equals(currentUser.getRole())) {
            var doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!report.getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only add prescriptions to your own reports.");
            }
        }

        Prescription prescription = Prescription.builder()
                .report(report)
                .drugName(request.getDrugName())
                .dosage(request.getDosage())
                .frequency(request.getFrequency())
                .duration(request.getDuration())
                .instructions(request.getInstructions())
                .medicationStartDate(request.getMedicationStartDate())
                .medicationEndDate(request.getMedicationEndDate())
                .refillsAllowed(request.getRefillsAllowed() != null ? request.getRefillsAllowed() : 0)
                .sideEffects(request.getSideEffects())
                .contraindications(request.getContraindications())
                .isActive(true)
                .drugReminderSent(false)
                .build();

        Prescription saved = prescriptionRepository.save(prescription);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Prescription added successfully", mapToResponse(saved)));
    }

    /**
     * GET /api/prescriptions/report/{reportId}
     * Requires: DOCTOR, PATIENT, PHARMACY, or ADMIN
     */
    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getPrescriptionsByReport(
            @PathVariable Long reportId) {
        List<PrescriptionResponse> prescriptions = prescriptionRepository
                .findByReportId(reportId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Prescriptions retrieved", prescriptions));
    }

    /**
     * GET /api/prescriptions/pharmacy/patient/{patientId}
     * Requires: PHARMACY or ADMIN — pharmacy can view active prescriptions by patient
     */
    @GetMapping("/pharmacy/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getActivePrescriptionsForPharmacy(
            @PathVariable Long patientId) {
        List<PrescriptionResponse> prescriptions = prescriptionRepository
                .findActivePrescriptionsByPatient(patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Active prescriptions retrieved", prescriptions));
    }

    /**
     * PUT /api/prescriptions/{id}/deactivate
     * Requires: DOCTOR or ADMIN — deactivates a prescription
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivatePrescription(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        if ("ROLE_DOCTOR".equals(currentUser.getRole())) {
            var doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!prescription.getReport().getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only modify your own prescriptions.");
            }
        }

        prescription.setIsActive(false);
        prescriptionRepository.save(prescription);
        return ResponseEntity.ok(ApiResponse.success("Prescription deactivated", null));
    }

    private PrescriptionResponse mapToResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId())
                .reportId(p.getReport().getId())
                .drugName(p.getDrugName())
                .dosage(p.getDosage())
                .frequency(p.getFrequency())
                .duration(p.getDuration())
                .instructions(p.getInstructions())
                .medicationStartDate(p.getMedicationStartDate())
                .medicationEndDate(p.getMedicationEndDate())
                .refillsAllowed(p.getRefillsAllowed())
                .isActive(p.getIsActive())
                .sideEffects(p.getSideEffects())
                .contraindications(p.getContraindications())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
