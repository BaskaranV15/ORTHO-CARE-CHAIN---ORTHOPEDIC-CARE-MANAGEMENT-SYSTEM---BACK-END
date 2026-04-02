package com.orthocarechain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "drug_name", nullable = false, length = 100)
    private String drugName;

    @Column(nullable = false, length = 50)
    private String dosage;

    @Column(nullable = false, length = 50)
    private String frequency;

    @Column(nullable = false, length = 50)
    private String duration;

    @Column(length = 500)
    private String instructions;

    @Column(name = "medication_start_date")
    private LocalDate medicationStartDate;

    @Column(name = "medication_end_date")
    private LocalDate medicationEndDate;

    @Column(name = "refills_allowed")
    @Builder.Default
    private Integer refillsAllowed = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "drug_reminder_sent")
    @Builder.Default
    private Boolean drugReminderSent = false;

    @Column(length = 500)
    private String sideEffects;

    @Column(length = 500)
    private String contraindications;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
