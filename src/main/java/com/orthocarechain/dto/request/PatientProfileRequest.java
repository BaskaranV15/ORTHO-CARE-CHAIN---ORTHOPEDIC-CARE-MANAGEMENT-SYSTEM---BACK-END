package com.orthocarechain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientProfileRequest {

    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood group format")
    private String bloodGroup;

    @Min(value = 30, message = "Height must be at least 30 cm")
    @Max(value = 300, message = "Height must not exceed 300 cm")
    private Double height;

    @Min(value = 1, message = "Weight must be positive")
    @Max(value = 700, message = "Weight must not exceed 700 kg")
    private Double weight;

    @Size(max = 500, message = "Allergies field must not exceed 500 characters")
    private String allergies;

    @Size(max = 500, message = "Chronic conditions field must not exceed 500 characters")
    private String chronicConditions;

    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Please provide a valid phone number")
    private String emergencyContactPhone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}
