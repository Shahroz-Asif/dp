package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CompatibilityResult {
    private Long recipeId;
    private String recipeName;
    private boolean recipeSelectable;
    private String reason;
    private List<ComponentCompatibility> componentDetails;
}
