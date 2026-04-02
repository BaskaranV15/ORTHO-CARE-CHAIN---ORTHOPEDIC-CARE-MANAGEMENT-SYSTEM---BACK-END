package com.orthocarechain.service.impl;

import com.orthocarechain.dto.request.PatientProfileRequest;
import com.orthocarechain.dto.response.PatientResponse;
import com.orthocarechain.entity.Patient;
import com.orthocarechain.entity.User;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.exception.UnauthorizedAccessException;
import com.orthocarechain.repository.PatientRepository;
import com.orthocarechain.repository.ReportRepository;
import com.orthocarechain.repository.UserRepository;
import com.orthocarechain.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public PatientResponse createOrUpdateProfile(PatientProfileRequest request) {
        UserDetailsImpl currentUser = getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Patient patient = patientRepository.findByUserId(user.getId())
                .orElse(Patient.builder().user(user).build());

        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setHeight(request.getHeight());
        patient.setWeight(request.getWeight());
        patient.setAllergies(request.getAllergies());
        patient.setChronic_conditions(request.getChronicConditions());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        patient.setAddress(request.getAddress());

        Patient saved = patientRepository.save(patient);
        log.info("Patient profile saved for user ID: {}", user.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse getMyProfile() {
        UserDetailsImpl currentUser = getCurrentUser();
        Patient patient = patientRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient profile not found. Please complete your profile."));
        return mapToResponse(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long patientId) {
        UserDetailsImpl currentUser = getCurrentUser();
        String role = currentUser.getRole();

        // Patients can only view their own profile
        if ("ROLE_PATIENT".equals(role)) {
            Patient myPatient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
            if (!myPatient.getId().equals(patientId)) {
                throw new UnauthorizedAccessException("You can only view your own profile.");
            }
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
        return mapToResponse(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAllActivePatients().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivatePatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
        patient.getUser().setIsActive(false);
        userRepository.save(patient.getUser());
        log.info("Patient deactivated: ID={}", patientId);
    }

    private PatientResponse mapToResponse(Patient patient) {
        Long totalReports = reportRepository.countByPatientId(patient.getId());
        return PatientResponse.builder()
                .id(patient.getId())
                .userId(patient.getUser().getId())
                .username(patient.getUser().getUsername())
                .email(patient.getUser().getEmail())
                .firstName(patient.getUser().getFirstName())
                .lastName(patient.getUser().getLastName())
                .phone(patient.getUser().getPhone())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .bloodGroup(patient.getBloodGroup())
                .height(patient.getHeight())
                .weight(patient.getWeight())
                .allergies(patient.getAllergies())
                .chronicConditions(patient.getChronic_conditions())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .address(patient.getAddress())
                .totalReports(totalReports)
                .createdAt(patient.getCreatedAt())
                .build();
    }

    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
