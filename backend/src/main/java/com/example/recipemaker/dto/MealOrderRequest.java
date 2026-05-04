package com.example.recipemaker.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class MealOrderRequest {
    private Long recipeId;
    /** IDs of the modifiable components the patient explicitly selected */
    private List<Long> selectedComponentIds = Collections.emptyList();
}
