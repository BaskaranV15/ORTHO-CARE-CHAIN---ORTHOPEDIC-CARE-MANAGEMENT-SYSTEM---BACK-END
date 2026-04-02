package com.orthocarechain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DoctorProfileRequest {

    @NotBlank(message = "Medical license number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String medicalLicenseNumber;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    private String specialization;

    @Size(max = 100, message = "Hospital name must not exceed 100 characters")
    private String hospital;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience seems too high")
    private Integer yearsOfExperience;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
}
