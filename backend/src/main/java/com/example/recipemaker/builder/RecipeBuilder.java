package com.example.recipemaker.builder;

import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.model.RecipeComponent;

import java.util.HashSet;
import java.util.Set;

/**
 * Builder pattern for constructing Recipe objects step-by-step.
 * Enforces that a main component must be set before building.
 */
public class RecipeBuilder {
    private String name;
    private String description;
    private RecipeComponent mainComponent;
    private final Set<RecipeComponent> modifiableComponents = new HashSet<>();

    public RecipeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public RecipeBuilder description(String description) {
        this.description = description;
        return this;
    }

    public RecipeBuilder mainComponent(RecipeComponent mainComponent) {
        if (mainComponent.isModifiable()) {
            throw new IllegalArgumentException("Main component must be non-modifiable");
        }
        this.mainComponent = mainComponent;
        return this;
    }

    public RecipeBuilder addModifiableComponent(RecipeComponent component) {
        if (!component.isModifiable()) {
            throw new IllegalArgumentException("Only modifiable components can be added as modifiable");
        }
        this.modifiableComponents.add(component);
        return this;
    }

    public RecipeBuilder addModifiableComponents(Set<RecipeComponent> components) {
        for (RecipeComponent c : components) {
            addModifiableComponent(c);
        }
        return this;
    }

    public Recipe build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Recipe name is required");
        }
        if (mainComponent == null) {
            throw new IllegalStateException("Main component is required");
        }
        return Recipe.builder()
                .name(name)
                .description(description)
                .mainComponent(mainComponent)
                .modifiableComponents(new HashSet<>(modifiableComponents))
                .build();
    }
}
