package com.orthocarechain.repository;

import com.orthocarechain.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    Optional<Doctor> findByMedicalLicenseNumber(String licenseNumber);

    Boolean existsByMedicalLicenseNumber(String licenseNumber);

    List<Doctor> findBySpecialization(String specialization);

    List<Doctor> findByHospital(String hospital);

    @Query("SELECT d FROM Doctor d WHERE d.user.isActive = true")
    List<Doctor> findAllActiveDoctors();

    @Query("SELECT d FROM Doctor d WHERE d.user.username = :username")
    Optional<Doctor> findByUsername(String username);
}
