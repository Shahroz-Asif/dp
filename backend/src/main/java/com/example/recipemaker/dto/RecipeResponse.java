package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class RecipeResponse {
    private Long id;
    private String name;
    private String description;
    private ComponentResponse mainComponent;
    private List<ComponentResponse> modifiableComponents;
}
