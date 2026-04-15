package com.example.recipemaker.controller;

import com.example.recipemaker.dto.PatientProfileRequest;
import com.example.recipemaker.dto.PatientProfileResponse;
import com.example.recipemaker.model.AppUser;
import com.example.recipemaker.model.PatientProfile;
import com.example.recipemaker.repository.AppUserRepository;
import com.example.recipemaker.repository.PatientProfileRepository;
import com.example.recipemaker.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Profiles", description = "Manage patient profiles")
public class PatientProfileController {
    private final PatientProfileRepository profileRepository;
    private final AppUserRepository userRepository;
    private final DoctorService doctorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "List all patient profiles (Doctor/Admin only)")
    public List<PatientProfileResponse> getAllProfiles() {
        return profileRepository.findAll().stream()
                .map(doctorService::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get current patient's own profile")
    public PatientProfileResponse getMyProfile(Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        PatientProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Profile not found"));
        return doctorService.toResponse(profile);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Get a patient profile by ID (Doctor/Admin only)")
    public PatientProfileResponse getProfile(@PathVariable Long id) {
        return doctorService.toResponse(profileRepository.findById(id).orElseThrow());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Create a new patient profile (Doctor/Admin only)")
    public PatientProfileResponse createProfile(@RequestBody PatientProfileRequest request,
                                                @RequestParam(required = false) Long userId) {
        PatientProfile profile = PatientProfile.builder()
                .name(request.getName())
                .age(request.getAge())
                .notes(request.getNotes())
                .build();
        if (userId != null) {
            AppUser user = userRepository.findById(userId).orElseThrow();
            profile.setUser(user);
        }
        return doctorService.toResponse(profileRepository.save(profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Update an existing patient profile (Doctor/Admin only)")
    public PatientProfileResponse updateProfile(@PathVariable Long id,
                                                @RequestBody PatientProfileRequest request) {
        PatientProfile existing = profileRepository.findById(id).orElseThrow();
        existing.setName(request.getName());
        existing.setAge(request.getAge());
        existing.setNotes(request.getNotes());
        return doctorService.toResponse(profileRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a patient profile (Admin only)")
    public void deleteProfile(@PathVariable Long id) {
        PatientProfile profile = profileRepository.findById(id).orElseThrow();
        profile.setDeleted(true);
        profileRepository.save(profile);
    }
}
