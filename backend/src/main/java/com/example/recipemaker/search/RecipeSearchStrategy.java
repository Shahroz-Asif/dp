package com.example.recipemaker.search;

import com.example.recipemaker.model.Recipe;

/**
 * Strategy interface for filtering recipes.
 * Part of the Composite Strategy pattern: individual strategies
 * can be composed into composite filters.
 */
public interface RecipeSearchStrategy {
    boolean matches(Recipe recipe);
}
