package com.orthocarechain.service.impl;

import com.orthocarechain.dto.request.PrescriptionRequest;
import com.orthocarechain.dto.request.ReportRequest;
import com.orthocarechain.dto.response.*;
import com.orthocarechain.entity.*;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.exception.UnauthorizedAccessException;
import com.orthocarechain.repository.*;
import com.orthocarechain.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl {

    private final ReportRepository reportRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalImageRepository medicalImageRepository;

    @Transactional
    public ReportResponse createReport(ReportRequest request) {
        UserDetailsImpl currentUser = getCurrentUser();

        // Get the doctor associated with the currently logged-in user
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for current user."));

        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        Report report = Report.builder()
                .doctor(doctor)
                .patient(patient)
                .visitDate(request.getVisitDate())
                .nextVisitDate(request.getNextVisitDate())
                .diagnosis(request.getDiagnosis())
                .diagnosisDetails(request.getDiagnosisDetails())
                .severityLevel(request.getSeverityLevel())
                .doctorNotes(request.getDoctorNotes())
                .treatmentPlan(request.getTreatmentPlan())
                .followUpInstructions(request.getFollowUpInstructions())
                .isActive(true)
                .visitReminderSent(false)
                .prescriptions(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        Report savedReport = reportRepository.save(report);

        // Save prescriptions if provided
        if (request.getPrescriptions() != null && !request.getPrescriptions().isEmpty()) {
            List<Prescription> prescriptions = request.getPrescriptions().stream()
                    .map(p -> mapToPrescription(p, savedReport))
                    .collect(Collectors.toList());
            prescriptionRepository.saveAll(prescriptions);
            savedReport.setPrescriptions(prescriptions);
        }

        log.info("Report created: ID={} by Doctor ID={} for Patient ID={}",
                savedReport.getId(), doctor.getId(), patient.getId());

        return mapToReportResponse(savedReport);
    }

    @Transactional
    public ReportResponse updateReport(Long reportId, ReportRequest request) {
        UserDetailsImpl currentUser = getCurrentUser();
        Report report = getReportWithOwnershipCheck(reportId, currentUser);

        report.setVisitDate(request.getVisitDate());
        report.setNextVisitDate(request.getNextVisitDate());
        report.setDiagnosis(request.getDiagnosis());
        report.setDiagnosisDetails(request.getDiagnosisDetails());
        report.setSeverityLevel(request.getSeverityLevel());
        report.setDoctorNotes(request.getDoctorNotes());
        report.setTreatmentPlan(request.getTreatmentPlan());
        report.setFollowUpInstructions(request.getFollowUpInstructions());
        report.setVisitReminderSent(false); // reset reminder if visit date changed

        Report updatedReport = reportRepository.save(report);
        log.info("Report updated: ID={}", reportId);
        return mapToReportResponse(updatedReport);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId) {
        UserDetailsImpl currentUser = getCurrentUser();
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        enforceReadAccess(report, currentUser);
        return mapToReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByPatient(Long patientId) {
        UserDetailsImpl currentUser = getCurrentUser();
        String role = currentUser.getRole();

        // Patients can only view their own reports
        if ("ROLE_PATIENT".equals(role)) {
            Patient patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
            if (!patient.getId().equals(patientId)) {
                throw new UnauthorizedAccessException("You can only view your own reports.");
            }
        }

        return reportRepository.findByPatientIdOrderByVisitDateDesc(patientId)
                .stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReportsAsDoctor() {
        UserDetailsImpl currentUser = getCurrentUser();
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
        return reportRepository.findByDoctorIdOrderByVisitDateDesc(doctor.getId())
                .stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReportsAsPatient() {
        UserDetailsImpl currentUser = getCurrentUser();
        Patient patient = patientRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
        return reportRepository.findActiveReportsByPatient(patient.getId())
                .stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReport(Long reportId) {
        UserDetailsImpl currentUser = getCurrentUser();
        String role = currentUser.getRole();

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        // Only the owning doctor or admin can delete
        if ("ROLE_DOCTOR".equals(role)) {
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!report.getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only delete your own reports.");
            }
        }

        // Soft delete
        report.setIsActive(false);
        reportRepository.save(report);
        log.info("Report soft-deleted: ID={}", reportId);
    }

    // ==================== HELPERS ====================

    private Report getReportWithOwnershipCheck(Long reportId, UserDetailsImpl currentUser) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        if ("ROLE_DOCTOR".equals(currentUser.getRole())) {
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!report.getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only modify your own reports.");
            }
        } else if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("Only doctors or admins can modify reports.");
        }
        return report;
    }

    private void enforceReadAccess(Report report, UserDetailsImpl currentUser) {
        String role = currentUser.getRole();
        if ("ROLE_PATIENT".equals(role)) {
            Patient patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
            if (!report.getPatient().getId().equals(patient.getId())) {
                throw new UnauthorizedAccessException("You can only view your own reports.");
            }
        } else if ("ROLE_DOCTOR".equals(role)) {
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!report.getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only view reports you created.");
            }
        } else if ("ROLE_PHARMACY".equals(role)) {
            // Pharmacy can only access prescription data - handled at controller level
            throw new UnauthorizedAccessException("Pharmacy users should access prescriptions endpoint instead.");
        }
    }

    private Prescription mapToPrescription(PrescriptionRequest req, Report report) {
        return Prescription.builder()
                .report(report)
                .drugName(req.getDrugName())
                .dosage(req.getDosage())
                .frequency(req.getFrequency())
                .duration(req.getDuration())
                .instructions(req.getInstructions())
                .medicationStartDate(req.getMedicationStartDate())
                .medicationEndDate(req.getMedicationEndDate())
                .refillsAllowed(req.getRefillsAllowed() != null ? req.getRefillsAllowed() : 0)
                .sideEffects(req.getSideEffects())
                .contraindications(req.getContraindications())
                .isActive(true)
                .drugReminderSent(false)
                .build();
    }

    public ReportResponse mapToReportResponse(Report report) {
        List<PrescriptionResponse> prescriptionResponses = new ArrayList<>();
        if (report.getPrescriptions() != null) {
            prescriptionResponses = report.getPrescriptions().stream()
                    .map(this::mapToPrescriptionResponse)
                    .collect(Collectors.toList());
        }

        List<MedicalImageResponse> imageResponses = new ArrayList<>();
        if (report.getImages() != null) {
            imageResponses = report.getImages().stream()
                    .map(this::mapToImageResponse)
                    .collect(Collectors.toList());
        }

        return ReportResponse.builder()
                .id(report.getId())
                .patientId(report.getPatient().getId())
                .patientName(report.getPatient().getUser().getFirstName() + " " +
                             report.getPatient().getUser().getLastName())
                .doctorId(report.getDoctor().getId())
                .doctorName("Dr. " + report.getDoctor().getUser().getFirstName() + " " +
                            report.getDoctor().getUser().getLastName())
                .doctorSpecialization(report.getDoctor().getSpecialization())
                .visitDate(report.getVisitDate())
                .nextVisitDate(report.getNextVisitDate())
                .diagnosis(report.getDiagnosis())
                .diagnosisDetails(report.getDiagnosisDetails())
                .severityLevel(report.getSeverityLevel())
                .doctorNotes(report.getDoctorNotes())
                .treatmentPlan(report.getTreatmentPlan())
                .followUpInstructions(report.getFollowUpInstructions())
                .isActive(report.getIsActive())
                .prescriptions(prescriptionResponses)
                .images(imageResponses)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private PrescriptionResponse mapToPrescriptionResponse(Prescription p) {
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

    private MedicalImageResponse mapToImageResponse(MedicalImage img) {
        return MedicalImageResponse.builder()
                .id(img.getId())
                .reportId(img.getReport().getId())
                .imageType(img.getImageType())
                .imageUrl(img.getImageUrl())
                .description(img.getDescription())
                .bodyPart(img.getBodyPart())
                .fileName(img.getFileName())
                .fileSize(img.getFileSize())
                .uploadedAt(img.getUploadedAt())
                .build();
    }

    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
