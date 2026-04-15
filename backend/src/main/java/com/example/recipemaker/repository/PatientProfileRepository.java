package com.example.recipemaker.repository;

import com.example.recipemaker.model.AppUser;
import com.example.recipemaker.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByUser(AppUser user);
    List<PatientProfile> findByAssignedDoctor(AppUser doctor);
}
