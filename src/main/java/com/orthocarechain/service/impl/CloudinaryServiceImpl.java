package com.orthocarechain.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orthocarechain.dto.response.MedicalImageResponse;
import com.orthocarechain.entity.MedicalImage;
import com.orthocarechain.entity.Report;
import com.orthocarechain.enums.ImageType;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.exception.UnauthorizedAccessException;
import com.orthocarechain.repository.DoctorRepository;
import com.orthocarechain.repository.MedicalImageRepository;
import com.orthocarechain.repository.ReportRepository;
import com.orthocarechain.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl {

    private final Cloudinary cloudinary;
    private final MedicalImageRepository medicalImageRepository;
    private final ReportRepository reportRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public MedicalImageResponse uploadMedicalImage(Long reportId,
                                                    MultipartFile file,
                                                    ImageType imageType,
                                                    String description,
                                                    String bodyPart) {
        UserDetailsImpl currentUser = getCurrentUser();

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        // Only the report's doctor or admin can upload images
        if ("ROLE_DOCTOR".equals(currentUser.getRole())) {
            var doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!report.getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only upload images to your own reports.");
            }
        }

        validateImageFile(file);

        try {
            // Upload to Cloudinary
            Map<String, String> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "orthocarechain/reports/" + reportId,
                            "resource_type", "image",
                            "quality", "auto",
                            "fetch_format", "auto",
                            "tags", List.of("medical", imageType.name().toLowerCase())
                    )
            );

            String imageUrl = uploadResult.get("secure_url");
            String publicId = uploadResult.get("public_id");

            MedicalImage medicalImage = MedicalImage.builder()
                    .report(report)
                    .imageType(imageType)
                    .imageUrl(imageUrl)
                    .cloudinaryPublicId(publicId)
                    .description(description)
                    .bodyPart(bodyPart)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .uploadedBy(currentUser.getId())
                    .build();

            MedicalImage saved = medicalImageRepository.save(medicalImage);
            log.info("Medical image uploaded: PublicID={} for Report ID={}", publicId, reportId);

            return mapToImageResponse(saved);

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image. Please try again.");
        }
    }

    @Transactional
    public void deleteImage(Long imageId) {
        UserDetailsImpl currentUser = getCurrentUser();

        MedicalImage image = medicalImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalImage", "id", imageId));

        // Authorization check
        if ("ROLE_DOCTOR".equals(currentUser.getRole())) {
            var doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            if (!image.getReport().getDoctor().getId().equals(doctor.getId())) {
                throw new UnauthorizedAccessException("You can only delete images from your own reports.");
            }
        }

        try {
            // Delete from Cloudinary
            if (image.getCloudinaryPublicId() != null) {
                cloudinary.uploader().destroy(image.getCloudinaryPublicId(),
                        ObjectUtils.asMap("resource_type", "image"));
            }
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", e.getMessage());
        }

        medicalImageRepository.delete(image);
        log.info("Medical image deleted: ID={}", imageId);
    }

    @Transactional(readOnly = true)
    public List<MedicalImageResponse> getImagesByReport(Long reportId) {
        return medicalImageRepository.findByReportId(reportId)
                .stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }
        long maxSize = 20 * 1024 * 1024; // 20MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Image file size must not exceed 20MB.");
        }
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
