//package com.orthocarechain.controller;
//
//import com.orthocarechain.dto.request.RegisterRequest;
//import com.orthocarechain.dto.response.ApiResponse;
//import com.orthocarechain.dto.response.UserResponse;
//import com.orthocarechain.entity.User;
//import com.orthocarechain.enums.Role;
//import com.orthocarechain.exception.DuplicateResourceException;
//import com.orthocarechain.exception.ResourceNotFoundException;
//import com.orthocarechain.repository.UserRepository;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/admin")
//@PreAuthorize("hasRole('ADMIN')")
//@RequiredArgsConstructor
//public class AdminController {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    /**
//     * GET /api/admin/users
//     * Lists all users in the system
//     */
//    @GetMapping("/users")
//    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
//        List<UserResponse> users = userRepository.findAll().stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//        return ResponseEntity.ok(ApiResponse.success("All users retrieved", users));
//    }
//
//    /**
//     * GET /api/admin/users/{id}
//     */
//    @GetMapping("/users/{id}")
//    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
//        return ResponseEntity.ok(ApiResponse.success("User retrieved", mapToResponse(user)));
//    }
//
//    /**
//     * POST /api/admin/users/create-admin
//     * Admin can create other admin accounts
//     */
//    @PostMapping("/users/create-admin")
//    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
//            @Valid @RequestBody RegisterRequest request) {
//        if (userRepository.existsByUsername(request.getUsername())) {
//            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken.");
//        }
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use.");
//        }
//
//        User user = User.builder()
//                .username(request.getUsername())
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(Role.ROLE_ADMIN)
//                .firstName(request.getFirstName())
//                .lastName(request.getLastName())
//                .phone(request.getPhone())
//                .isActive(true)
//                .isEmailVerified(true)
//                .build();
//
//        User saved = userRepository.save(user);
//        return ResponseEntity.ok(ApiResponse.success("Admin user created successfully", mapToResponse(saved)));
//    }
//
//    /**
//     * PUT /api/admin/users/{id}/toggle-active
//     * Enable or disable any user account
//     */
//    @PutMapping("/users/{id}/toggle-active")
//    public ResponseEntity<ApiResponse<UserResponse>> toggleUserActive(@PathVariable Long id) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
//        user.setIsActive(!user.getIsActive());
//        User saved = userRepository.save(user);
//        String status = saved.getIsActive() ? "activated" : "deactivated";
//        return ResponseEntity.ok(ApiResponse.success("User " + status + " successfully", mapToResponse(saved)));
//    }
//
//    /**
//     * GET /api/admin/users/by-role/{role}
//     */
//    @GetMapping("/users/by-role/{role}")
//    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable Role role) {
//        List<UserResponse> users = userRepository.findByRole(role).stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//        return ResponseEntity.ok(ApiResponse.success("Users by role retrieved", users));
//    }
//
//    /**
//     * DELETE /api/admin/users/{id}
//     * Hard delete user (use with caution)
//     */
//    @DeleteMapping("/users/{id}")
//    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
//        if (!userRepository.existsById(id)) {
//            throw new ResourceNotFoundException("User", "id", id);
//        }
//        userRepository.deleteById(id);
//        return ResponseEntity.ok(ApiResponse.success("User deleted permanently", null));
//    }
//
//    /**
//     * GET /api/admin/stats
//     * System statistics
//     */
//    @GetMapping("/stats")
//    public ResponseEntity<ApiResponse<Object>> getStats() {
//        long totalUsers = userRepository.count();
//        long doctors = userRepository.findByRole(Role.ROLE_DOCTOR).size();
//        long patients = userRepository.findByRole(Role.ROLE_PATIENT).size();
//        long pharmacies = userRepository.findByRole(Role.ROLE_PHARMACY).size();
//
//        var stats = java.util.Map.of(
//                "totalUsers", totalUsers,
//                "totalDoctors", doctors,
//                "totalPatients", patients,
//                "totalPharmacies", pharmacies
//        );
//        return ResponseEntity.ok(ApiResponse.success("System statistics", stats));
//    }
//
//    private UserResponse mapToResponse(User user) {
//        return UserResponse.builder()
//                .id(user.getId())
//                .username(user.getUsername())
//                .email(user.getEmail())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .phone(user.getPhone())
//                .role(user.getRole())
//                .isActive(user.getIsActive())
//                .isEmailVerified(user.getIsEmailVerified())
//                .createdAt(user.getCreatedAt())
//                .build();
//    }
//}



package com.orthocarechain.controller;

import com.orthocarechain.dto.request.RegisterRequest;
import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.UserResponse;
import com.orthocarechain.entity.Doctor;
import com.orthocarechain.entity.Patient;
import com.orthocarechain.entity.Report;
import com.orthocarechain.entity.User;
import com.orthocarechain.enums.Role;
import com.orthocarechain.exception.DuplicateResourceException;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.repository.DoctorRepository;
import com.orthocarechain.repository.PatientRepository;
import com.orthocarechain.repository.ReportRepository;
import com.orthocarechain.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final DoctorRepository   doctorRepository;
    private final PatientRepository  patientRepository;
    private final ReportRepository   reportRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/users  — list all users
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All users retrieved", users));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/users/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(ApiResponse.success("User retrieved", mapToResponse(user)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/users/create-admin  — always creates with ROLE_ADMIN
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/users/create-admin")
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
            @Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken.");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use.");

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_ADMIN)          // always ADMIN — never from request
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .isActive(true)
                .isEmailVerified(true)
                .build();

        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Admin user created successfully", mapToResponse(saved)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/admin/users/{id}/toggle-active
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(!user.getIsActive());
        User saved = userRepository.save(user);
        String status = saved.getIsActive() ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.success("User " + status + " successfully", mapToResponse(saved)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/users/by-role/{role}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/users/by-role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable Role role) {
        List<UserResponse> users = userRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Users by role retrieved", users));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/admin/users/{id}
    //
    // Deletion order (required by FK constraints):
    //   1. prescriptions & medical_images  — auto-cascaded from Report
    //   2. reports linked to doctor
    //   3. reports linked to patient
    //   4. doctor profile
    //   5. patient profile
    //   6. user
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // ── 1. If user is a DOCTOR: delete reports first, then profile ────────
        doctorRepository.findByUserId(id).ifPresent(doctor -> {
            // findByDoctorId fetches all reports; cascade ALL on Report
            // will automatically delete prescriptions + medical_images
            List<Report> doctorReports = reportRepository.findByDoctorIdOrderByVisitDateDesc(doctor.getId());
            reportRepository.deleteAll(doctorReports);
            doctorRepository.delete(doctor);
        });

        // ── 2. If user is a PATIENT: delete reports first, then profile ───────
        patientRepository.findByUserId(id).ifPresent(patient -> {
            List<Report> patientReports = reportRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
            reportRepository.deleteAll(patientReports);
            patientRepository.delete(patient);
        });

        // ── 3. Finally delete the user row ────────────────────────────────────
        userRepository.delete(user);

        return ResponseEntity.ok(ApiResponse.success("User deleted permanently", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/stats
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getStats() {
        long totalUsers   = userRepository.count();
        long doctors      = userRepository.findByRole(Role.ROLE_DOCTOR).size();
        long patients     = userRepository.findByRole(Role.ROLE_PATIENT).size();
        long pharmacies   = userRepository.findByRole(Role.ROLE_PHARMACY).size();

        var stats = java.util.Map.of(
                "totalUsers",      totalUsers,
                "totalDoctors",    doctors,
                "totalPatients",   patients,
                "totalPharmacies", pharmacies
        );
        return ResponseEntity.ok(ApiResponse.success("System statistics", stats));
    }

    // ─────────────────────────────────────────────────────────────────────────
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}