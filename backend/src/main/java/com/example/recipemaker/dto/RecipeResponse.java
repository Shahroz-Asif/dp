package com.example.recipemaker.dto;

import com.example.recipemaker.model.MealCourse;
import com.example.recipemaker.model.MealType;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RecipeResponse {
    private Long id;
    private String name;
    private String description;
    private MealCourse mealCourse;
    private MealType mealType;
    private String createdByUsername;
    private ComponentResponse mainComponent;
    private List<ComponentResponse> modifiableComponents;
}
