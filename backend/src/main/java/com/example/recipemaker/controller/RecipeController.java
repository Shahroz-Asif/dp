package com.example.recipemaker.controller;

import com.example.recipemaker.dto.CompatibilityResult;
import com.example.recipemaker.dto.RecipeRequest;
import com.example.recipemaker.dto.RecipeResponse;
import com.example.recipemaker.model.MealCourse;
import com.example.recipemaker.model.MealType;
import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "Recipes", description = "Recipe management and compatibility checks")
public class RecipeController {
    private final RecipeService recipeService;

    @GetMapping
    @Operation(summary = "List all recipes (optionally filter by mealCourse and mealType)")
    public List<RecipeResponse> getAllRecipes(
            @RequestParam(required = false) MealCourse mealCourse,
            @RequestParam(required = false) MealType mealType) {
        return recipeService.getAllRecipes().stream()
                .filter(r -> mealCourse == null || mealCourse.equals(r.getMealCourse()))
                .filter(r -> mealType == null || mealType.equals(r.getMealType()))
                .map(recipeService::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a recipe by ID")
    public RecipeResponse getRecipe(@PathVariable Long id) {
        return recipeService.toResponse(recipeService.getRecipeById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DIETICIAN','ADMIN')")
    @Operation(summary = "Create a recipe (Dietician/Admin only)")
    public RecipeResponse createRecipe(@RequestBody RecipeRequest request) {
        Recipe recipe = recipeService.createRecipe(request);
        return recipeService.toResponse(recipe);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIETICIAN','ADMIN')")
    @Operation(summary = "Update an existing recipe (Dietician/Admin only)")
    public RecipeResponse updateRecipe(@PathVariable Long id, @RequestBody RecipeRequest request) {
        Recipe recipe = recipeService.updateRecipe(id, request);
        return recipeService.toResponse(recipe);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('DIETICIAN','ADMIN')")
    @Operation(summary = "Delete a recipe (Dietician/Admin only)")
    public void deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
    }

    @PostMapping("/{id}/check-compatibility")
    @Operation(summary = "Check recipe compatibility with patient conditions")
    public CompatibilityResult checkCompatibility(@PathVariable Long id, @RequestBody Set<Long> conditionIds) {
        return recipeService.checkCompatibility(id, conditionIds);
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter recipes using Composite Strategy pattern")
    public List<RecipeResponse> searchRecipes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String componentName,
            @RequestParam(required = false) Set<Long> compatibleConditionIds) {
        return recipeService.searchRecipes(name, componentName, compatibleConditionIds).stream()
                .map(recipeService::toResponse)
                .collect(Collectors.toList());
    }
}
