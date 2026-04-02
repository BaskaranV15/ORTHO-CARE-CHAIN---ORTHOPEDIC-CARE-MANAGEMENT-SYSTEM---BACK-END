package com.orthocarechain.repository;

import com.orthocarechain.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    @Query("SELECT p FROM Patient p WHERE p.user.isActive = true")
    List<Patient> findAllActivePatients();

    @Query("SELECT p FROM Patient p WHERE p.user.username = :username")
    Optional<Patient> findByUsername(String username);

    List<Patient> findByBloodGroup(String bloodGroup);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.user.isActive = true")
    Long countActivePatients();
}
