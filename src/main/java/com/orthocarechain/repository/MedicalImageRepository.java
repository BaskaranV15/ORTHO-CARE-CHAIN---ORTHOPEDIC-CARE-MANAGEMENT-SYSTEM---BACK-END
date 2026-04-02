package com.orthocarechain.repository;

import com.orthocarechain.entity.MedicalImage;
import com.orthocarechain.enums.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalImageRepository extends JpaRepository<MedicalImage, Long> {

    List<MedicalImage> findByReportId(Long reportId);

    List<MedicalImage> findByReportIdAndImageType(Long reportId, ImageType imageType);

    Optional<MedicalImage> findByCloudinaryPublicId(String publicId);

    @Query("SELECT mi FROM MedicalImage mi WHERE mi.report.patient.id = :patientId")
    List<MedicalImage> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT mi FROM MedicalImage mi WHERE mi.report.patient.id = :patientId AND mi.imageType = :imageType")
    List<MedicalImage> findByPatientIdAndImageType(@Param("patientId") Long patientId,
                                                   @Param("imageType") ImageType imageType);

    Long countByReportId(Long reportId);
}
