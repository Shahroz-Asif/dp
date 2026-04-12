package com.example.recipemaker.search;

import com.example.recipemaker.model.Recipe;
import java.util.ArrayList;
import java.util.List;

/**
 * Composite Strategy: composes multiple RecipeSearchStrategy instances.
 * A recipe matches if ANY child strategy matches (OR logic).
 */
public class CompositeOrSearchStrategy implements RecipeSearchStrategy {
    private final List<RecipeSearchStrategy> strategies = new ArrayList<>();

    public CompositeOrSearchStrategy add(RecipeSearchStrategy strategy) {
        strategies.add(strategy);
        return this;
    }

    @Override
    public boolean matches(Recipe recipe) {
        if (strategies.isEmpty()) return true;
        for (RecipeSearchStrategy strategy : strategies) {
            if (strategy.matches(recipe)) {
                return true;
            }
        }
        return false;
    }
}
