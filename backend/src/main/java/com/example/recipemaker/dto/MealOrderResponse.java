package com.example.recipemaker.dto;

import com.example.recipemaker.model.MealCourse;
import com.example.recipemaker.model.MealType;
import com.example.recipemaker.model.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MealOrderResponse {
    private Long id;
    private Long patientProfileId;
    private String patientName;
    private Long recipeId;
    private String recipeName;
    private MealCourse mealCourse;
    private MealType mealType;
    private OrderStatus status;
    private LocalDate orderDate;
    private LocalDateTime createdAt;
}
