package com.example.recipemaker.repository;

import com.example.recipemaker.model.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientConditionRepository extends JpaRepository<PatientCondition, Long> {
}
