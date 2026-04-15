package com.example.recipemaker.dto;

import com.example.recipemaker.model.MealCourse;
import com.example.recipemaker.model.MealType;
import lombok.Data;
import java.util.Set;

@Data
public class RecipeRequest {
    private String name;
    private String description;
    private MealCourse mealCourse;
    private MealType mealType;
    private Long mainComponentId;
    private Set<Long> modifiableComponentIds;
}
