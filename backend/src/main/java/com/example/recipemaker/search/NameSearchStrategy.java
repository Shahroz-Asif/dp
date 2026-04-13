package com.example.recipemaker.search;

import com.example.recipemaker.model.Recipe;

public class NameSearchStrategy implements RecipeSearchStrategy {
    private final String keyword;

    public NameSearchStrategy(String keyword) {
        this.keyword = keyword.toLowerCase();
    }

    @Override
    public boolean matches(Recipe recipe) {
        return recipe.getName() != null
                && recipe.getName().toLowerCase().contains(keyword);
    }
}
