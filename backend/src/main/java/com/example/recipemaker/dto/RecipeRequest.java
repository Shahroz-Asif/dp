package com.example.recipemaker.dto;

import lombok.Data;
import java.util.Set;

@Data
public class RecipeRequest {
    private String name;
    private String description;
    private Long mainComponentId;
    private Set<Long> modifiableComponentIds;
}
