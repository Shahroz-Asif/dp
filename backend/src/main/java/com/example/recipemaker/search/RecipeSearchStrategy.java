package com.example.recipemaker.search;

import com.example.recipemaker.model.Recipe;

public interface RecipeSearchStrategy {
    boolean matches(Recipe recipe);
}
