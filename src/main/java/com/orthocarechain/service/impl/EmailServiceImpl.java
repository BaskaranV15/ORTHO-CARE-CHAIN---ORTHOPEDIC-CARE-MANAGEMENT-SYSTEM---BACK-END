package com.orthocarechain.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl {

    private final JavaMailSender mailSender;

    @Async
    public void sendDrugReminderEmail(String to, String patientName,
                                       String drugName, String dosage,
                                       String frequency, String instructions) {
        String subject = "OrthoCareChain - Medication Reminder: " + drugName;
        String body = buildDrugReminderBody(patientName, drugName, dosage, frequency, instructions);
        sendEmail(to, subject, body);
    }

    @Async
    public void sendVisitReminderEmail(String to, String patientName,
                                        String doctorName, String visitDate,
                                        String diagnosis) {
        String subject = "OrthoCareChain - Upcoming Visit Reminder";
        String body = buildVisitReminderBody(patientName, doctorName, visitDate, diagnosis);
        sendEmail(to, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String to, String name, String role) {
        String subject = "Welcome to OrthoCareChain!";
        String body = buildWelcomeEmailBody(name, role);
        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildDrugReminderBody(String patientName, String drugName,
                                          String dosage, String frequency,
                                          String instructions) {
        return """
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
            <div style="background:#1a73e8; padding:20px; border-radius:8px 8px 0 0;">
                <h1 style="color:white; margin:0;">💊 OrthoCareChain</h1>
                <p style="color:white; margin:5px 0 0;">Medication Reminder</p>
            </div>
            <div style="border:1px solid #ddd; border-top:none; padding:20px; border-radius:0 0 8px 8px;">
                <p>Dear <strong>%s</strong>,</p>
                <p>This is your daily medication reminder from OrthoCareChain.</p>
                <div style="background:#f8f9fa; padding:15px; border-radius:8px; border-left:4px solid #1a73e8;">
                    <h3 style="margin:0 0 10px; color:#1a73e8;">Medication Details</h3>
                    <p><strong>Drug:</strong> %s</p>
                    <p><strong>Dosage:</strong> %s</p>
                    <p><strong>Frequency:</strong> %s</p>
                    <p><strong>Instructions:</strong> %s</p>
                </div>
                <p style="color:#666; font-size:12px; margin-top:20px;">
                    Please consult your doctor before making any changes to your medication.
                </p>
            </div>
            </body></html>
            """.formatted(patientName, drugName, dosage, frequency,
                          instructions != null ? instructions : "As directed");
    }

    private String buildVisitReminderBody(String patientName, String doctorName,
                                           String visitDate, String diagnosis) {
        return """
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
            <div style="background:#34a853; padding:20px; border-radius:8px 8px 0 0;">
                <h1 style="color:white; margin:0;">🏥 OrthoCareChain</h1>
                <p style="color:white; margin:5px 0 0;">Visit Reminder</p>
            </div>
            <div style="border:1px solid #ddd; border-top:none; padding:20px; border-radius:0 0 8px 8px;">
                <p>Dear <strong>%s</strong>,</p>
                <p>This is a reminder that you have an upcoming visit tomorrow.</p>
                <div style="background:#f8f9fa; padding:15px; border-radius:8px; border-left:4px solid #34a853;">
                    <h3 style="margin:0 0 10px; color:#34a853;">Visit Details</h3>
                    <p><strong>Doctor:</strong> %s</p>
                    <p><strong>Date:</strong> %s</p>
                    <p><strong>Reason:</strong> %s</p>
                </div>
                <p>Please remember to bring your previous reports and any recent test results.</p>
                <p style="color:#666; font-size:12px; margin-top:20px;">
                    Contact your clinic to reschedule if needed.
                </p>
            </div>
            </body></html>
            """.formatted(patientName, doctorName, visitDate,
                          diagnosis != null ? diagnosis : "Follow-up visit");
    }

    private String buildWelcomeEmailBody(String name, String role) {
        return """
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
            <div style="background:#1a73e8; padding:20px; border-radius:8px 8px 0 0;">
                <h1 style="color:white; margin:0;">🔗 OrthoCareChain</h1>
                <p style="color:white; margin:5px 0 0;">Welcome!</p>
            </div>
            <div style="border:1px solid #ddd; border-top:none; padding:20px; border-radius:0 0 8px 8px;">
                <p>Dear <strong>%s</strong>,</p>
                <p>Welcome to <strong>OrthoCareChain</strong> — your secure orthopedic healthcare management system.</p>
                <p>You have been registered as a <strong>%s</strong>.</p>
                <p>You can now log in and access your dashboard to manage healthcare records securely.</p>
                <p style="color:#666; font-size:12px; margin-top:20px;">
                    If you did not create this account, please contact support immediately.
                </p>
            </div>
            </body></html>
            """.formatted(name, role.replace("ROLE_", "").toLowerCase());
    }
}
