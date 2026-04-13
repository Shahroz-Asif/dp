package com.example.recipemaker.search;

import com.example.recipemaker.model.Recipe;
import java.util.ArrayList;
import java.util.List;

public class CompositeAndSearchStrategy implements RecipeSearchStrategy {
    private final List<RecipeSearchStrategy> strategies = new ArrayList<>();

    public CompositeAndSearchStrategy add(RecipeSearchStrategy strategy) {
        strategies.add(strategy);
        return this;
    }

    @Override
    public boolean matches(Recipe recipe) {
        for (RecipeSearchStrategy strategy : strategies) {
            if (!strategy.matches(recipe)) {
                return false;
            }
        }
        return true;
    }
}
