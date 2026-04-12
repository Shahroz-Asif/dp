package com.example.recipemaker.controller;

import com.example.recipemaker.model.RecipeComponent;
import com.example.recipemaker.repository.RecipeComponentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Tag(name = "Components", description = "Manage recipe components (modifiable and non-modifiable)")
public class ComponentController {
    private final RecipeComponentRepository componentRepository;

    @GetMapping
    @Operation(summary = "List all components")
    public List<RecipeComponent> getAllComponents() {
        return componentRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a component by ID")
    public RecipeComponent getComponent(@PathVariable Long id) {
        return componentRepository.findById(id).orElseThrow();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new component")
    public RecipeComponent createComponent(@RequestBody RecipeComponent component) {
        return componentRepository.save(component);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing component")
    public RecipeComponent updateComponent(@PathVariable Long id, @RequestBody RecipeComponent component) {
        component.setId(id);
        return componentRepository.save(component);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a component")
    public void deleteComponent(@PathVariable Long id) {
        componentRepository.deleteById(id);
    }
}
