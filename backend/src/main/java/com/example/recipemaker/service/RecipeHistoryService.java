package com.example.recipemaker.service;

import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeHistoryService {
    private final RecipeRepository recipeRepository;

    public List<Recipe> getRecipeHistory(Long recipeId) {
        return recipeRepository.findAll();
    }
}
