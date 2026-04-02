package com.orthocarechain.controller;

import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.MedicalImageResponse;
import com.orthocarechain.enums.ImageType;
import com.orthocarechain.service.impl.CloudinaryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class MedicalImageController {

    private final CloudinaryServiceImpl cloudinaryService;

    /**
     * POST /api/images/upload/{reportId}
     * Requires: DOCTOR (own reports only) or ADMIN
     * Uploads medical image to Cloudinary and saves URL in DB
     */
    @PostMapping("/upload/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<MedicalImageResponse>> uploadImage(
            @PathVariable Long reportId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") ImageType imageType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "bodyPart", required = false) String bodyPart) {

        MedicalImageResponse image = cloudinaryService.uploadMedicalImage(
                reportId, file, imageType, description, bodyPart);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", image));
    }

    /**
     * GET /api/images/report/{reportId}
     * Requires: DOCTOR, PATIENT, or ADMIN — fetches all images for a report
     */
    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MedicalImageResponse>>> getImagesByReport(
            @PathVariable Long reportId) {
        List<MedicalImageResponse> images = cloudinaryService.getImagesByReport(reportId);
        return ResponseEntity.ok(ApiResponse.success("Images retrieved", images));
    }

    /**
     * DELETE /api/images/{imageId}
     * Requires: DOCTOR (own reports only) or ADMIN
     * Deletes from Cloudinary and DB
     */
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long imageId) {
        cloudinaryService.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }
}
