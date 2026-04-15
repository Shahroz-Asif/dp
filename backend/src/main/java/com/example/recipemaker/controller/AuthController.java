package com.example.recipemaker.controller;

import com.example.recipemaker.dto.UserResponse;
import com.example.recipemaker.model.AppUser;
import com.example.recipemaker.model.PatientProfile;
import com.example.recipemaker.repository.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and authentication")
public class AuthController {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user (defaults to PATIENT role)")
    public String register(@RequestBody AppUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("PATIENT");
        }
        userRepository.save(user);
        return "User registered";
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public UserResponse me(Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow();
        PatientProfile profile = user.getPatientProfile();
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .patientProfileId(profile != null ? profile.getId() : null)
                .build();
    }
}
