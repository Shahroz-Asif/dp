package com.example.recipemaker.repository;

import com.example.recipemaker.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealOrderRepository extends JpaRepository<MealOrder, Long> {
    List<MealOrder> findByPatientAndStatusNotOrderByCreatedAtDesc(PatientProfile patient, OrderStatus status);
    List<MealOrder> findByPatientAndStatusOrderByCreatedAtDesc(PatientProfile patient, OrderStatus status);
    List<MealOrder> findByStatusNotOrderByCreatedAtDesc(OrderStatus status);
    long countByPatientAndOrderDateAndRecipe_MealCourseAndRecipe_MealType(
            PatientProfile patient, LocalDate orderDate, MealCourse mealCourse, MealType mealType);
}
