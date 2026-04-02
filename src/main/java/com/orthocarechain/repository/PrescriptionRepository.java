package com.orthocarechain.repository;

import com.orthocarechain.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByReportId(Long reportId);

    List<Prescription> findByReportIdAndIsActive(Long reportId, Boolean isActive);

    @Query("SELECT p FROM Prescription p WHERE p.report.patient.id = :patientId AND p.isActive = true")
    List<Prescription> findActivePrescriptionsByPatient(@Param("patientId") Long patientId);

    // Active prescriptions whose end date is today or in the future
    @Query("SELECT p FROM Prescription p WHERE p.medicationEndDate >= :today AND p.isActive = true AND p.drugReminderSent = false")
    List<Prescription> findActivePrescriptionsForReminder(@Param("today") LocalDate today);

    @Query("SELECT p FROM Prescription p WHERE p.report.id IN " +
           "(SELECT r.id FROM Report r WHERE r.doctor.id = :doctorId)")
    List<Prescription> findByDoctorId(@Param("doctorId") Long doctorId);

    List<Prescription> findByDrugNameContainingIgnoreCase(String drugName);
}
