package com.example.recipemaker.controller;

import com.example.recipemaker.dto.PatientProfileResponse;
import com.example.recipemaker.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Tag(name = "Doctor", description = "Doctor-patient management")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Get doctor's assigned patients")
    public List<PatientProfileResponse> getMyPatients() {
        return doctorService.getDoctorPatients();
    }

    @PostMapping("/patients/{patientId}/conditions/{conditionId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Assign a condition to a patient")
    public PatientProfileResponse assignCondition(@PathVariable Long patientId,
                                                  @PathVariable Long conditionId) {
        return doctorService.assignCondition(patientId, conditionId);
    }

    @DeleteMapping("/patients/{patientId}/conditions/{conditionId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Remove a condition from a patient")
    public PatientProfileResponse removeCondition(@PathVariable Long patientId,
                                                  @PathVariable Long conditionId) {
        return doctorService.removeCondition(patientId, conditionId);
    }
}
