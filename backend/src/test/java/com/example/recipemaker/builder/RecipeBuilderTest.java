package com.example.recipemaker.builder;

import com.example.recipemaker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecipeBuilderTest {

    private NonModifiableComponent mainComp;
    private ModifiableComponent modComp1;
    private ModifiableComponent modComp2;

    @BeforeEach
    void setUp() {
        mainComp = new NonModifiableComponent();
        mainComp.setId(1L);
        mainComp.setName("Grilled Chicken");
        mainComp.setDescription("Plain chicken");
        mainComp.setIncompatibleConditions(Set.of());

        modComp1 = new ModifiableComponent();
        modComp1.setId(2L);
        modComp1.setName("Cream Sauce");
        modComp1.setDescription("Dairy sauce");
        modComp1.setIncompatibleConditions(Set.of());

        modComp2 = new ModifiableComponent();
        modComp2.setId(3L);
        modComp2.setName("Steamed Broccoli");
        modComp2.setDescription("Veggies");
        modComp2.setIncompatibleConditions(Set.of());
    }

    @Test
    void buildRecipeWithAllFields() {
        Recipe recipe = new RecipeBuilder()
                .name("Test Recipe")
                .description("A test recipe")
                .mainComponent(mainComp)
                .addModifiableComponent(modComp1)
                .addModifiableComponent(modComp2)
                .build();

        assertEquals("Test Recipe", recipe.getName());
        assertEquals("A test recipe", recipe.getDescription());
        assertEquals(mainComp, recipe.getMainComponent());
        assertEquals(2, recipe.getModifiableComponents().size());
        assertTrue(recipe.getModifiableComponents().contains(modComp1));
        assertTrue(recipe.getModifiableComponents().contains(modComp2));
    }

    @Test
    void buildRecipeWithNoModifiableComponents() {
        Recipe recipe = new RecipeBuilder()
                .name("Simple Recipe")
                .mainComponent(mainComp)
                .build();

        assertEquals("Simple Recipe", recipe.getName());
        assertNull(recipe.getDescription());
        assertEquals(mainComp, recipe.getMainComponent());
        assertTrue(recipe.getModifiableComponents().isEmpty());
    }

    @Test
    void buildRecipeUsingAddModifiableComponents() {
        Recipe recipe = new RecipeBuilder()
                .name("Batch Add Recipe")
                .mainComponent(mainComp)
                .addModifiableComponents(Set.of(modComp1, modComp2))
                .build();

        assertEquals(2, recipe.getModifiableComponents().size());
    }

    @Test
    void buildFailsWithoutName() {
        RecipeBuilder builder = new RecipeBuilder()
                .mainComponent(mainComp);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Recipe name is required", ex.getMessage());
    }

    @Test
    void buildFailsWithBlankName() {
        RecipeBuilder builder = new RecipeBuilder()
                .name("   ")
                .mainComponent(mainComp);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Recipe name is required", ex.getMessage());
    }

    @Test
    void buildFailsWithoutMainComponent() {
        RecipeBuilder builder = new RecipeBuilder()
                .name("No Main");

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Main component is required", ex.getMessage());
    }

    @Test
    void mainComponentRejectsModifiableComponent() {
        RecipeBuilder builder = new RecipeBuilder().name("Bad");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> builder.mainComponent(modComp1));
        assertEquals("Main component must be non-modifiable", ex.getMessage());
    }

    @Test
    void addModifiableRejectsNonModifiableComponent() {
        RecipeBuilder builder = new RecipeBuilder()
                .name("Bad")
                .mainComponent(mainComp);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> builder.addModifiableComponent(mainComp));
        assertEquals("Only modifiable components can be added as modifiable", ex.getMessage());
    }

    @Test
    void getAllComponentsIncludesMainAndModifiable() {
        Recipe recipe = new RecipeBuilder()
                .name("Full Recipe")
                .mainComponent(mainComp)
                .addModifiableComponent(modComp1)
                .build();

        Set<RecipeComponent> all = recipe.getAllComponents();
        assertEquals(2, all.size());
        assertTrue(all.contains(mainComp));
        assertTrue(all.contains(modComp1));
    }
}
