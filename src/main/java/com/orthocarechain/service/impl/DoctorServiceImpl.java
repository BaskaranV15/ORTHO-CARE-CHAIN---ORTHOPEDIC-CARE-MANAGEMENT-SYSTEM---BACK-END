package com.orthocarechain.service.impl;

import com.orthocarechain.dto.request.DoctorProfileRequest;
import com.orthocarechain.dto.response.DoctorResponse;
import com.orthocarechain.entity.Doctor;
import com.orthocarechain.entity.User;
import com.orthocarechain.exception.DuplicateResourceException;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.exception.UnauthorizedAccessException;
import com.orthocarechain.repository.DoctorRepository;
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
public class DoctorServiceImpl {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public DoctorResponse createOrUpdateProfile(DoctorProfileRequest request) {
        UserDetailsImpl currentUser = getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Check if license already used by someone else
        doctorRepository.findByMedicalLicenseNumber(request.getMedicalLicenseNumber())
                .ifPresent(existing -> {
                    if (!existing.getUser().getId().equals(user.getId())) {
                        throw new DuplicateResourceException(
                                "Medical license number '" + request.getMedicalLicenseNumber() + "' is already registered.");
                    }
                });

        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElse(Doctor.builder().user(user).build());

        doctor.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setHospital(request.getHospital());
        doctor.setDepartment(request.getDepartment());
        doctor.setYearsOfExperience(request.getYearsOfExperience());
        doctor.setBio(request.getBio());

        Doctor saved = doctorRepository.save(doctor);
        log.info("Doctor profile saved for user ID: {}", user.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getMyProfile() {
        UserDetailsImpl currentUser = getCurrentUser();
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found. Please complete your profile."));
        return mapToResponse(doctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        return mapToResponse(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAllActiveDoctors().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        doctor.getUser().setIsActive(false);
        userRepository.save(doctor.getUser());
        log.info("Doctor deactivated: ID={}", doctorId);
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        Long totalReports = reportRepository.countByDoctorId(doctor.getId());
        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(doctor.getUser().getId())
                .username(doctor.getUser().getUsername())
                .email(doctor.getUser().getEmail())
                .firstName(doctor.getUser().getFirstName())
                .lastName(doctor.getUser().getLastName())
                .phone(doctor.getUser().getPhone())
                .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
                .specialization(doctor.getSpecialization())
                .hospital(doctor.getHospital())
                .department(doctor.getDepartment())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .bio(doctor.getBio())
                .totalReports(totalReports)
                .createdAt(doctor.getCreatedAt())
                .build();
    }

    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
