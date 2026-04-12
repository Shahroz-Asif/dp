package com.example.recipemaker.controller;

import com.example.recipemaker.model.PatientProfile;
import com.example.recipemaker.repository.PatientProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Profiles", description = "Manage patient profiles")
public class PatientProfileController {
    private final PatientProfileRepository profileRepository;

    @GetMapping
    @Operation(summary = "List all patient profiles")
    public List<PatientProfile> getAllProfiles() {
        return profileRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a patient profile by ID")
    public PatientProfile getProfile(@PathVariable Long id) {
        return profileRepository.findById(id).orElseThrow();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new patient profile")
    public PatientProfile createProfile(@RequestBody PatientProfile profile) {
        return profileRepository.save(profile);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing patient profile")
    public PatientProfile updateProfile(@PathVariable Long id, @RequestBody PatientProfile profile) {
        profile.setId(id);
        return profileRepository.save(profile);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a patient profile")
    public void deleteProfile(@PathVariable Long id) {
        profileRepository.deleteById(id);
    }
}
