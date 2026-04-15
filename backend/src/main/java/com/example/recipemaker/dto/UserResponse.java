package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private Long patientProfileId;
}
