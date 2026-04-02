package com.orthocarechain.repository;

import com.orthocarechain.entity.Report;
import com.orthocarechain.enums.SeverityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByPatientIdOrderByVisitDateDesc(Long patientId);

    List<Report> findByDoctorIdOrderByVisitDateDesc(Long doctorId);

    List<Report> findByPatientIdAndDoctorId(Long patientId, Long doctorId);

    List<Report> findBySeverityLevel(SeverityLevel severityLevel);

    @Query("SELECT r FROM Report r WHERE r.patient.id = :patientId AND r.isActive = true ORDER BY r.visitDate DESC")
    List<Report> findActiveReportsByPatient(@Param("patientId") Long patientId);

    @Query("SELECT r FROM Report r WHERE r.doctor.id = :doctorId AND r.isActive = true ORDER BY r.visitDate DESC")
    List<Report> findActiveReportsByDoctor(@Param("doctorId") Long doctorId);

    // For visit reminders - next visit is tomorrow and reminder not sent
    @Query("SELECT r FROM Report r WHERE r.nextVisitDate = :tomorrow AND r.visitReminderSent = false AND r.isActive = true")
    List<Report> findReportsWithUpcomingVisit(@Param("tomorrow") LocalDate tomorrow);

    // For visit reminders on a specific day range
    @Query("SELECT r FROM Report r WHERE r.nextVisitDate BETWEEN :startDate AND :endDate AND r.visitReminderSent = false")
    List<Report> findReportsWithVisitBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Report r WHERE r.patient.id = :patientId AND r.visitDate = :visitDate")
    Optional<Report> findByPatientIdAndVisitDate(@Param("patientId") Long patientId, @Param("visitDate") LocalDate visitDate);

    Long countByDoctorId(Long doctorId);

    Long countByPatientId(Long patientId);

    @Query("SELECT r FROM Report r WHERE r.doctor.id = :doctorId AND r.patient.id = :patientId ORDER BY r.visitDate DESC")
    List<Report> findByDoctorAndPatient(@Param("doctorId") Long doctorId, @Param("patientId") Long patientId);
}
