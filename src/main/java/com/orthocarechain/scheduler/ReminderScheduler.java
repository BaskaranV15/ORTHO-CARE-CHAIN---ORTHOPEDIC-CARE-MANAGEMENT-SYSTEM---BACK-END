package com.orthocarechain.scheduler;

import com.orthocarechain.entity.Prescription;
import com.orthocarechain.entity.Report;
import com.orthocarechain.repository.PrescriptionRepository;
import com.orthocarechain.repository.ReportRepository;
import com.orthocarechain.service.impl.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ReportRepository reportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final EmailServiceImpl emailService;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Sends visit reminders every day at 8:00 AM for appointments scheduled for tomorrow.
     */
    @Scheduled(cron = "${scheduler.email.reminder.cron}")
    @Transactional
    public void sendVisitReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("Running visit reminder scheduler for date: {}", tomorrow);

        List<Report> upcomingReports = reportRepository.findReportsWithUpcomingVisit(tomorrow);
        log.info("Found {} reports with upcoming visits.", upcomingReports.size());

        for (Report report : upcomingReports) {
            try {
                String patientEmail = report.getPatient().getUser().getEmail();
                String patientName = report.getPatient().getUser().getFirstName() + " " +
                                     report.getPatient().getUser().getLastName();
                String doctorName = "Dr. " + report.getDoctor().getUser().getFirstName() + " " +
                                    report.getDoctor().getUser().getLastName();
                String visitDateStr = report.getNextVisitDate().format(DATE_FORMATTER);

                emailService.sendVisitReminderEmail(
                        patientEmail,
                        patientName,
                        doctorName,
                        visitDateStr,
                        report.getDiagnosis()
                );

                // Mark reminder as sent to avoid duplicate emails
                report.setVisitReminderSent(true);
                reportRepository.save(report);

                log.info("Visit reminder sent to: {} for date: {}", patientEmail, visitDateStr);
            } catch (Exception e) {
                log.error("Failed to send visit reminder for report ID {}: {}",
                        report.getId(), e.getMessage());
            }
        }
    }

    /**
     * Sends drug intake reminders every day at 8:00 AM for active prescriptions.
     */
    @Scheduled(cron = "${scheduler.email.reminder.cron}")
    @Transactional
    public void sendDrugReminders() {
        LocalDate today = LocalDate.now();
        log.info("Running drug reminder scheduler for date: {}", today);

        List<Prescription> activePrescriptions =
                prescriptionRepository.findActivePrescriptionsForReminder(today);
        log.info("Found {} active prescriptions for reminders.", activePrescriptions.size());

        for (Prescription prescription : activePrescriptions) {
            try {
                String patientEmail = prescription.getReport().getPatient().getUser().getEmail();
                String patientName = prescription.getReport().getPatient().getUser().getFirstName() +
                                     " " + prescription.getReport().getPatient().getUser().getLastName();

                emailService.sendDrugReminderEmail(
                        patientEmail,
                        patientName,
                        prescription.getDrugName(),
                        prescription.getDosage(),
                        prescription.getFrequency(),
                        prescription.getInstructions()
                );

                prescription.setDrugReminderSent(true);
                prescriptionRepository.save(prescription);

                log.info("Drug reminder sent to: {} for: {}", patientEmail, prescription.getDrugName());
            } catch (Exception e) {
                log.error("Failed to send drug reminder for prescription ID {}: {}",
                        prescription.getId(), e.getMessage());
            }
        }
    }
}
