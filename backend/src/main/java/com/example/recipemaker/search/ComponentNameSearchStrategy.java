package com.example.recipemaker.search;

import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.model.RecipeComponent;

/**
 * Strategy: matches recipes that contain a component with the given name.
 */
public class ComponentNameSearchStrategy implements RecipeSearchStrategy {
    private final String componentName;

    public ComponentNameSearchStrategy(String componentName) {
        this.componentName = componentName.toLowerCase();
    }

    @Override
    public boolean matches(Recipe recipe) {
        for (RecipeComponent c : recipe.getAllComponents()) {
            if (c.getName() != null && c.getName().toLowerCase().contains(componentName)) {
                return true;
            }
        }
        return false;
    }
}
