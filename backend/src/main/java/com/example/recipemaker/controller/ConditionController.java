package com.example.recipemaker.controller;

import com.example.recipemaker.model.PatientCondition;
import com.example.recipemaker.repository.PatientConditionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conditions")
@RequiredArgsConstructor
@Tag(name = "Patient Conditions", description = "Manage patient conditions for incompatibility tracking")
public class ConditionController {
    private final PatientConditionRepository conditionRepository;

    @GetMapping
    @Operation(summary = "List all patient conditions")
    public List<PatientCondition> getAllConditions() {
        return conditionRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a condition by ID")
    public PatientCondition getCondition(@PathVariable Long id) {
        return conditionRepository.findById(id).orElseThrow();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new patient condition")
    public PatientCondition createCondition(@RequestBody PatientCondition condition) {
        return conditionRepository.save(condition);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing condition")
    public PatientCondition updateCondition(@PathVariable Long id, @RequestBody PatientCondition condition) {
        condition.setId(id);
        return conditionRepository.save(condition);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a condition")
    public void deleteCondition(@PathVariable Long id) {
        conditionRepository.deleteById(id);
    }
}
