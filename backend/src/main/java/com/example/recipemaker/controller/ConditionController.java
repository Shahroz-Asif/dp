package com.example.recipemaker.controller;

import com.example.recipemaker.dto.PatientConditionResponse;
import com.example.recipemaker.model.AppUser;
import com.example.recipemaker.model.PatientCondition;
import com.example.recipemaker.repository.AppUserRepository;
import com.example.recipemaker.repository.PatientConditionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conditions")
@RequiredArgsConstructor
@Tag(name = "Patient Conditions", description = "Manage patient conditions for incompatibility tracking")
public class ConditionController {
    private final PatientConditionRepository conditionRepository;
    private final AppUserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List all patient conditions")
    public List<PatientConditionResponse> getAllConditions() {
        return conditionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a condition by ID")
    public PatientConditionResponse getCondition(@PathVariable Long id) {
        return toResponse(conditionRepository.findById(id).orElseThrow());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Create a new patient condition (Doctor/Admin only)")
    public PatientConditionResponse createCondition(@RequestBody PatientCondition condition,
                                                    Authentication authentication) {
        AppUser creator = userRepository.findByUsername(authentication.getName()).orElseThrow();
        if ("DOCTOR".equals(creator.getRole())) {
            condition.setCreatedByDoctor(creator);
        }
        return toResponse(conditionRepository.save(condition));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Soft-delete a condition (Doctor/Admin only)")
    public void deleteCondition(@PathVariable Long id, Authentication authentication) {
        PatientCondition condition = conditionRepository.findById(id).orElseThrow();
        AppUser requester = userRepository.findByUsername(authentication.getName()).orElseThrow();
        if ("DOCTOR".equals(requester.getRole()) && condition.getCreatedByDoctor() != null
                && !condition.getCreatedByDoctor().getId().equals(requester.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can only delete conditions you created.");
        }
        condition.setDeleted(true);
        conditionRepository.save(condition);
    }

    private PatientConditionResponse toResponse(PatientCondition condition) {
        return PatientConditionResponse.builder()
                .id(condition.getId())
                .name(condition.getName())
                .description(condition.getDescription())
                .createdByDoctorName(condition.getCreatedByDoctor() != null
                        ? condition.getCreatedByDoctor().getUsername() : null)
                .build();
    }
}
