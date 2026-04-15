package com.example.recipemaker.repository;

import com.example.recipemaker.model.AppUser;
import com.example.recipemaker.model.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientConditionRepository extends JpaRepository<PatientCondition, Long> {
    List<PatientCondition> findByCreatedByDoctorId(Long doctorId);
}
