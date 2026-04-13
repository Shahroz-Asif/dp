package com.example.recipemaker.service;

import com.example.recipemaker.builder.RecipeBuilder;
import com.example.recipemaker.dto.*;
import com.example.recipemaker.model.PatientCondition;
import com.example.recipemaker.model.Recipe;
import com.example.recipemaker.model.RecipeComponent;
import com.example.recipemaker.repository.PatientConditionRepository;
import com.example.recipemaker.repository.RecipeComponentRepository;
import com.example.recipemaker.repository.RecipeRepository;
import com.example.recipemaker.search.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipeComponentRepository componentRepository;
    private final PatientConditionRepository conditionRepository;

    public Recipe createRecipe(RecipeRequest request) {
        RecipeComponent mainComponent = componentRepository.findById(request.getMainComponentId())
                .orElseThrow(() -> new NoSuchElementException("Main component not found"));

        RecipeBuilder builder = new RecipeBuilder()
                .name(request.getName())
                .description(request.getDescription())
                .mainComponent(mainComponent);

        if (request.getModifiableComponentIds() != null) {
            Set<RecipeComponent> modifiables = request.getModifiableComponentIds().stream()
                    .map(id -> componentRepository.findById(id)
                            .orElseThrow(() -> new NoSuchElementException("Component not found: " + id)))
                    .collect(Collectors.toSet());
            builder.addModifiableComponents(modifiables);
        }

        Recipe recipe = builder.build();
        return recipeRepository.save(recipe);
    }

    public Recipe getRecipeById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recipe not found"));
    }

    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    public Recipe updateRecipe(Long id, RecipeRequest request) {
        Recipe existing = getRecipeById(id);
        RecipeComponent mainComponent = componentRepository.findById(request.getMainComponentId())
                .orElseThrow(() -> new NoSuchElementException("Main component not found"));

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setMainComponent(mainComponent);

        if (request.getModifiableComponentIds() != null) {
            Set<RecipeComponent> modifiables = request.getModifiableComponentIds().stream()
                    .map(cid -> componentRepository.findById(cid)
                            .orElseThrow(() -> new NoSuchElementException("Component not found: " + cid)))
                    .collect(Collectors.toSet());
            existing.setModifiableComponents(modifiables);
        }

        return recipeRepository.save(existing);
    }

    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
    }

    public CompatibilityResult checkCompatibility(Long recipeId, Set<Long> conditionIds) {
        Recipe recipe = getRecipeById(recipeId);
        Set<PatientCondition> conditions = new HashSet<>(conditionRepository.findAllById(conditionIds));

        List<ComponentCompatibility> details = new ArrayList<>();
        boolean recipeSelectable = true;
        String reason = "Compatible";

        RecipeComponent main = recipe.getMainComponent();
        String mainIncompat = findIncompatibleCondition(main, conditions);
        if (mainIncompat != null) {
            recipeSelectable = false;
            reason = "Main component '" + main.getName() + "' is incompatible with condition: " + mainIncompat;
        }
        details.add(ComponentCompatibility.builder()
                .componentId(main.getId())
                .componentName(main.getName())
                .modifiable(false)
                .selectable(mainIncompat == null)
                .incompatibleCondition(mainIncompat)
                .build());

        for (RecipeComponent comp : recipe.getModifiableComponents()) {
            String incompat = findIncompatibleCondition(comp, conditions);
            details.add(ComponentCompatibility.builder()
                    .componentId(comp.getId())
                    .componentName(comp.getName())
                    .modifiable(true)
                    .selectable(incompat == null)
                    .incompatibleCondition(incompat)
                    .build());
        }

        return CompatibilityResult.builder()
                .recipeId(recipe.getId())
                .recipeName(recipe.getName())
                .recipeSelectable(recipeSelectable)
                .reason(reason)
                .componentDetails(details)
                .build();
    }

    private String findIncompatibleCondition(RecipeComponent component, Set<PatientCondition> conditions) {
        if (component.getIncompatibleConditions() == null) return null;
        for (PatientCondition incompatibleCondition : component.getIncompatibleConditions()) {
            for (PatientCondition patientCondition : conditions) {
                if (incompatibleCondition.getId().equals(patientCondition.getId())) {
                    return patientCondition.getName();
                }
            }
        }
        return null;
    }

    public List<Recipe> searchRecipes(String name, String componentName, Set<Long> compatibleConditionIds) {
        CompositeAndSearchStrategy composite = new CompositeAndSearchStrategy();

        if (name != null && !name.isBlank()) {
            composite.add(new NameSearchStrategy(name));
        }
        if (componentName != null && !componentName.isBlank()) {
            composite.add(new ComponentNameSearchStrategy(componentName));
        }
        if (compatibleConditionIds != null && !compatibleConditionIds.isEmpty()) {
            Set<PatientCondition> conditions = new HashSet<>(conditionRepository.findAllById(compatibleConditionIds));
            composite.add(new ConditionCompatibilityStrategy(conditions));
        }

        return recipeRepository.findAll().stream()
                .filter(composite::matches)
                .collect(Collectors.toList());
    }

    public RecipeResponse toResponse(Recipe recipe) {
        return RecipeResponse.builder()
                .id(recipe.getId())
                .name(recipe.getName())
                .description(recipe.getDescription())
                .mainComponent(toComponentResponse(recipe.getMainComponent()))
                .modifiableComponents(recipe.getModifiableComponents().stream()
                        .map(this::toComponentResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private ComponentResponse toComponentResponse(RecipeComponent component) {
        return ComponentResponse.builder()
                .id(component.getId())
                .name(component.getName())
                .description(component.getDescription())
                .modifiable(component.isModifiable())
                .incompatibleConditionNames(
                        component.getIncompatibleConditions() != null
                                ? component.getIncompatibleConditions().stream()
                                .map(PatientCondition::getName)
                                .collect(Collectors.toSet())
                                : Set.of())
                .build();
    }
}
