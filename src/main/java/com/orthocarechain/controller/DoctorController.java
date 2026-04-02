package com.orthocarechain.controller;

import com.orthocarechain.dto.request.DoctorProfileRequest;
import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.DoctorResponse;
import com.orthocarechain.service.impl.DoctorServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorServiceImpl doctorService;

    /**
     * POST /api/doctors/profile
     * Requires: DOCTOR — creates or updates own professional profile
     */
    @PostMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> createOrUpdateProfile(
            @Valid @RequestBody DoctorProfileRequest request) {
        DoctorResponse doctor = doctorService.createOrUpdateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Doctor profile saved successfully", doctor));
    }

    /**
     * GET /api/doctors/profile/me
     * Requires: DOCTOR — retrieves own profile
     */
    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> getMyProfile() {
        DoctorResponse doctor = doctorService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Doctor profile retrieved", doctor));
    }

    /**
     * GET /api/doctors/{id}
     * Requires: DOCTOR, PATIENT, ADMIN — view any doctor's public profile
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        DoctorResponse doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor profile retrieved", doctor));
    }

    /**
     * GET /api/doctors
     * Requires: ADMIN or DOCTOR — list all active doctors
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.success("Doctors list retrieved", doctors));
    }

    /**
     * GET /api/doctors/specialization/{spec}
     * Requires: Any authenticated user — search doctors by specialization
     */
    @GetMapping("/specialization/{spec}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctorsBySpecialization(
            @PathVariable String spec) {
        List<DoctorResponse> doctors = doctorService.getDoctorsBySpecialization(spec);
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved by specialization", doctors));
    }

    /**
     * DELETE /api/doctors/{id}/deactivate
     * Requires: ADMIN only — deactivates a doctor's account
     */
    @DeleteMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateDoctor(@PathVariable Long id) {
        doctorService.deactivateDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor deactivated successfully", null));
    }
}
