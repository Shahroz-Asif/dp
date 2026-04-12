package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComponentCompatibility {
    private Long componentId;
    private String componentName;
    private boolean modifiable;
    private boolean selectable;
    private String incompatibleCondition;
}
