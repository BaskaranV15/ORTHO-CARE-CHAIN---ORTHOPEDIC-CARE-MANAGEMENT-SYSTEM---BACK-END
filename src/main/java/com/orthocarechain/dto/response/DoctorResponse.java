package com.orthocarechain.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String medicalLicenseNumber;
    private String specialization;
    private String hospital;
    private String department;
    private Integer yearsOfExperience;
    private String bio;
    private Long totalReports;
    private LocalDateTime createdAt;
}
