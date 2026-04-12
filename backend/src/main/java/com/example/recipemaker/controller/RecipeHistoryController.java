package com.example.recipemaker.controller;

import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.service.RecipeHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recipes/history")
@RequiredArgsConstructor
@Tag(name = "Recipe History", description = "Track recipe version history")
public class RecipeHistoryController {
    private final RecipeHistoryService historyService;

    @GetMapping("/{id}")
    @Operation(summary = "Get the history of a recipe")
    public List<Recipe> getRecipeHistory(@PathVariable Long id) {
        return historyService.getRecipeHistory(id);
    }
}
