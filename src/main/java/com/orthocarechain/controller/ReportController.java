package com.orthocarechain.controller;

import com.orthocarechain.dto.request.ReportRequest;
import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.ReportResponse;
import com.orthocarechain.service.impl.PdfServiceImpl;
import com.orthocarechain.service.impl.ReportServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;
    private final PdfServiceImpl pdfService;

    /**
     * POST /api/reports
     * Requires: DOCTOR or ADMIN
     * Creates a new visit report for a patient
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody ReportRequest request) {
        ReportResponse report = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Report created successfully", report));
    }

    /**
     * PUT /api/reports/{id}
     * Requires: DOCTOR (own reports only) or ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request) {
        ReportResponse report = reportService.updateReport(id, request);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", report));
    }

    /**
     * GET /api/reports/{id}
     * Requires: DOCTOR (own reports), PATIENT (own reports), or ADMIN
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long id) {
        ReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Report retrieved", report));
    }

    /**
     * GET /api/reports/all
     * Requires: ADMIN only
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getAllReports() {
        List<ReportResponse> reports = reportService.getAllReports();
        return ResponseEntity.ok(ApiResponse.success("All reports retrieved", reports));
    }

    /**
     * GET /api/reports/my-reports
     * Requires: PATIENT — returns their own reports only
     */
    @GetMapping("/my-reports")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReportsAsPatient() {
        List<ReportResponse> reports = reportService.getMyReportsAsPatient();
        return ResponseEntity.ok(ApiResponse.success("Your reports retrieved", reports));
    }

    /**
     * GET /api/reports/doctor/my-reports
     * Requires: DOCTOR — returns reports created by this doctor only
     */
    @GetMapping("/doctor/my-reports")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReportsAsDoctor() {
        List<ReportResponse> reports = reportService.getMyReportsAsDoctor();
        return ResponseEntity.ok(ApiResponse.success("Your reports retrieved", reports));
    }

    /**
     * GET /api/reports/patient/{patientId}
     * Requires: DOCTOR (any), ADMIN, or PATIENT (own data only)
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByPatient(
            @PathVariable Long patientId) {
        List<ReportResponse> reports = reportService.getReportsByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.success("Patient reports retrieved", reports));
    }

    /**
     * DELETE /api/reports/{id}
     * Requires: DOCTOR (own reports only) or ADMIN
     * Performs soft delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully", null));
    }

    /**
     * GET /api/reports/{id}/download-pdf
     * Requires: DOCTOR (own reports), PATIENT (own reports), or ADMIN
     * Downloads report as PDF
     */
    @GetMapping("/{id}/download-pdf")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.generateReportPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "report-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
