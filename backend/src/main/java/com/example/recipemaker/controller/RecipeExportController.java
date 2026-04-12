package com.example.recipemaker.controller;

import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.repository.RecipeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/recipes/export")
@RequiredArgsConstructor
@Tag(name = "Recipe Export/Import", description = "Export and import recipes")
public class RecipeExportController {
    private final RecipeRepository recipeRepository;

    @GetMapping
    @Operation(summary = "Export all recipes as JSON")
    public List<Recipe> exportAllRecipes() {
        return recipeRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Import recipes from JSON")
    public ResponseEntity<String> importRecipes(@RequestBody List<Recipe> recipes) {
        recipeRepository.saveAll(recipes);
        return new ResponseEntity<>("Recipes imported", HttpStatus.CREATED);
    }
}
