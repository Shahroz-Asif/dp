package com.example.recipemaker.dto;

import lombok.Data;

@Data
public class PatientProfileRequest {
    private String name;
    private int age;
    private String notes;
}
