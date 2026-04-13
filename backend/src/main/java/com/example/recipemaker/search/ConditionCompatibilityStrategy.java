package com.example.recipemaker.search;

import com.example.recipemaker.model.PatientCondition;
import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.model.RecipeComponent;

import java.util.Set;

public class ConditionCompatibilityStrategy implements RecipeSearchStrategy {
    private final Set<PatientCondition> patientConditions;

    public ConditionCompatibilityStrategy(Set<PatientCondition> patientConditions) {
        this.patientConditions = patientConditions;
    }

    @Override
    public boolean matches(Recipe recipe) {
        RecipeComponent main = recipe.getMainComponent();
        if (main == null) return false;
        for (PatientCondition condition : main.getIncompatibleConditions()) {
            if (patientConditions.contains(condition)) {
                return false;
            }
        }
        return true;
    }
}
