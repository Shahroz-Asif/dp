package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class ComponentResponse {
    private Long id;
    private String name;
    private String description;
    private boolean modifiable;
    private Set<String> incompatibleConditionNames;
}
