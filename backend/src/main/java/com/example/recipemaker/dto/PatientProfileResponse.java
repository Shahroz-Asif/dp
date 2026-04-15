package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PatientProfileResponse {
    private Long id;
    private String name;
    private int age;
    private String notes;
    private String assignedDoctorUsername;
    private List<PatientConditionResponse> conditions;
}
