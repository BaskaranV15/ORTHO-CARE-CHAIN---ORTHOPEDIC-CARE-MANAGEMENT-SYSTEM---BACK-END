package com.orthocarechain.controller;

import com.orthocarechain.dto.request.PatientProfileRequest;
import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.PatientResponse;
import com.orthocarechain.service.impl.PatientServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientServiceImpl patientService;

    /**
     * POST /api/patients/profile
     * Requires: PATIENT — creates or updates own medical profile
     */
    @PostMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> createOrUpdateProfile(
            @Valid @RequestBody PatientProfileRequest request) {
        PatientResponse patient = patientService.createOrUpdateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Patient profile saved successfully", patient));
    }

    /**
     * GET /api/patients/profile/me
     * Requires: PATIENT — retrieves own profile
     */
    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile() {
        PatientResponse patient = patientService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Patient profile retrieved", patient));
    }

    /**
     * GET /api/patients/{id}
     * Requires: DOCTOR or ADMIN — view patient profile
     * PATIENT can only view their own (enforced in service)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT','PHARMACY')")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable Long id) {
        PatientResponse patient = patientService.getPatientById(id);
        return ResponseEntity.ok(ApiResponse.success("Patient profile retrieved", patient));
    }

    /**
     * GET /api/patients
     * Requires: DOCTOR or ADMIN — list all patients
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN','PHARMACY')")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        List<PatientResponse> patients = patientService.getAllPatients();
        return ResponseEntity.ok(ApiResponse.success("Patients list retrieved", patients));
    }

    /**
     * DELETE /api/patients/{id}/deactivate
     * Requires: ADMIN only
     */
    @DeleteMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivatePatient(@PathVariable Long id) {
        patientService.deactivatePatient(id);
        return ResponseEntity.ok(ApiResponse.success("Patient deactivated successfully", null));
    }
}
