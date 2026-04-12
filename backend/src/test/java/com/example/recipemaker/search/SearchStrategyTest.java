package com.example.recipemaker.search;

import com.example.recipemaker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SearchStrategyTest {

    private PatientCondition celiac;
    private PatientCondition lactose;
    private PatientCondition diabetes;

    private NonModifiableComponent safeMain;
    private NonModifiableComponent glutenMain;
    private ModifiableComponent dairySub;
    private ModifiableComponent safeSub;

    private Recipe safeRecipe;
    private Recipe glutenRecipe;
    private Recipe mixedRecipe;

    @BeforeEach
    void setUp() {
        celiac = PatientCondition.builder().id(1L).name("Celiac Disease").build();
        lactose = PatientCondition.builder().id(2L).name("Lactose Intolerance").build();
        diabetes = PatientCondition.builder().id(3L).name("Type 2 Diabetes").build();

        safeMain = new NonModifiableComponent();
        safeMain.setId(1L);
        safeMain.setName("Grilled Chicken");
        safeMain.setIncompatibleConditions(Set.of());

        glutenMain = new NonModifiableComponent();
        glutenMain.setId(2L);
        glutenMain.setName("Wheat Pasta Base");
        glutenMain.setIncompatibleConditions(Set.of(celiac));

        dairySub = new ModifiableComponent();
        dairySub.setId(3L);
        dairySub.setName("Cream Sauce");
        dairySub.setIncompatibleConditions(Set.of(lactose));

        safeSub = new ModifiableComponent();
        safeSub.setId(4L);
        safeSub.setName("Steamed Broccoli");
        safeSub.setIncompatibleConditions(Set.of());

        safeRecipe = Recipe.builder()
                .id(1L).name("Chicken & Broccoli")
                .mainComponent(safeMain)
                .modifiableComponents(Set.of(safeSub))
                .build();

        glutenRecipe = Recipe.builder()
                .id(2L).name("Pasta Alfredo")
                .mainComponent(glutenMain)
                .modifiableComponents(Set.of(dairySub))
                .build();

        mixedRecipe = Recipe.builder()
                .id(3L).name("Chicken Comfort Plate")
                .mainComponent(safeMain)
                .modifiableComponents(Set.of(dairySub, safeSub))
                .build();
    }

    // === NameSearchStrategy ===

    @Test
    void nameSearch_matchesExactSubstring() {
        NameSearchStrategy strategy = new NameSearchStrategy("Chicken");
        assertTrue(strategy.matches(safeRecipe));
        assertFalse(strategy.matches(glutenRecipe));
        assertTrue(strategy.matches(mixedRecipe));
    }

    @Test
    void nameSearch_caseInsensitive() {
        NameSearchStrategy strategy = new NameSearchStrategy("pasta");
        assertTrue(strategy.matches(glutenRecipe));
        assertFalse(strategy.matches(safeRecipe));
    }

    @Test
    void nameSearch_noMatchReturnsEmpty() {
        NameSearchStrategy strategy = new NameSearchStrategy("Pizza");
        assertFalse(strategy.matches(safeRecipe));
        assertFalse(strategy.matches(glutenRecipe));
        assertFalse(strategy.matches(mixedRecipe));
    }

    // === ComponentNameSearchStrategy ===

    @Test
    void componentNameSearch_matchesMainComponent() {
        ComponentNameSearchStrategy strategy = new ComponentNameSearchStrategy("Wheat Pasta");
        assertTrue(strategy.matches(glutenRecipe));
        assertFalse(strategy.matches(safeRecipe));
    }

    @Test
    void componentNameSearch_matchesModifiableComponent() {
        ComponentNameSearchStrategy strategy = new ComponentNameSearchStrategy("Cream Sauce");
        assertTrue(strategy.matches(glutenRecipe));
        assertTrue(strategy.matches(mixedRecipe));
        assertFalse(strategy.matches(safeRecipe));
    }

    @Test
    void componentNameSearch_matchesSafeSubComponent() {
        ComponentNameSearchStrategy strategy = new ComponentNameSearchStrategy("Broccoli");
        assertTrue(strategy.matches(safeRecipe));
        assertFalse(strategy.matches(glutenRecipe));
        assertTrue(strategy.matches(mixedRecipe));
    }

    // === ConditionCompatibilityStrategy ===

    @Test
    void conditionCompat_celiacBlocksGlutenRecipe() {
        ConditionCompatibilityStrategy strategy = new ConditionCompatibilityStrategy(Set.of(celiac));
        assertTrue(strategy.matches(safeRecipe));   // safe main
        assertFalse(strategy.matches(glutenRecipe)); // wheat main blocked
        assertTrue(strategy.matches(mixedRecipe));   // chicken main is safe
    }

    @Test
    void conditionCompat_lactoseDoesNotBlockMainComponent() {
        // Lactose only affects subcomponents, not the main components in our data
        ConditionCompatibilityStrategy strategy = new ConditionCompatibilityStrategy(Set.of(lactose));
        assertTrue(strategy.matches(safeRecipe));
        assertTrue(strategy.matches(glutenRecipe)); // wheat main is not lactose-incompatible
        assertTrue(strategy.matches(mixedRecipe));
    }

    @Test
    void conditionCompat_multipleConditions() {
        ConditionCompatibilityStrategy strategy = new ConditionCompatibilityStrategy(Set.of(celiac, lactose));
        assertTrue(strategy.matches(safeRecipe));    // chicken — fine
        assertFalse(strategy.matches(glutenRecipe)); // wheat — celiac blocks
        assertTrue(strategy.matches(mixedRecipe));   // chicken — fine
    }

    @Test
    void conditionCompat_noConditionsMatchesAll() {
        ConditionCompatibilityStrategy strategy = new ConditionCompatibilityStrategy(Set.of());
        assertTrue(strategy.matches(safeRecipe));
        assertTrue(strategy.matches(glutenRecipe));
        assertTrue(strategy.matches(mixedRecipe));
    }

    // === CompositeAndSearchStrategy ===

    @Test
    void compositeAnd_allStrategiesMustMatch() {
        CompositeAndSearchStrategy composite = new CompositeAndSearchStrategy()
                .add(new NameSearchStrategy("Chicken"))
                .add(new ConditionCompatibilityStrategy(Set.of(celiac)));

        assertTrue(composite.matches(safeRecipe));   // name OK, main OK
        assertFalse(composite.matches(glutenRecipe)); // name NO
        assertTrue(composite.matches(mixedRecipe));   // name OK, main OK
    }

    @Test
    void compositeAnd_emptyCompositMatchesEverything() {
        CompositeAndSearchStrategy composite = new CompositeAndSearchStrategy();
        assertTrue(composite.matches(safeRecipe));
        assertTrue(composite.matches(glutenRecipe));
        assertTrue(composite.matches(mixedRecipe));
    }

    @Test
    void compositeAnd_singleStrategyBehavesLikeWrapped() {
        CompositeAndSearchStrategy composite = new CompositeAndSearchStrategy()
                .add(new NameSearchStrategy("Pasta"));

        assertFalse(composite.matches(safeRecipe));
        assertTrue(composite.matches(glutenRecipe));
        assertFalse(composite.matches(mixedRecipe));
    }

    @Test
    void compositeAnd_threeFiltersNarrowsResults() {
        CompositeAndSearchStrategy composite = new CompositeAndSearchStrategy()
                .add(new NameSearchStrategy("Chicken"))
                .add(new ComponentNameSearchStrategy("Broccoli"))
                .add(new ConditionCompatibilityStrategy(Set.of(celiac)));

        assertTrue(composite.matches(safeRecipe));    // all three match
        assertFalse(composite.matches(glutenRecipe)); // name and component fail
        assertTrue(composite.matches(mixedRecipe));   // all three match
    }

    // === CompositeOrSearchStrategy ===

    @Test
    void compositeOr_anyStrategyCanMatch() {
        CompositeOrSearchStrategy composite = new CompositeOrSearchStrategy()
                .add(new NameSearchStrategy("Chicken"))
                .add(new NameSearchStrategy("Pasta"));

        assertTrue(composite.matches(safeRecipe));   // Chicken
        assertTrue(composite.matches(glutenRecipe)); // Pasta
        assertTrue(composite.matches(mixedRecipe));  // Chicken
    }

    @Test
    void compositeOr_emptyCompositeMatchesAll() {
        CompositeOrSearchStrategy composite = new CompositeOrSearchStrategy();
        assertTrue(composite.matches(safeRecipe));
    }

    @Test
    void compositeOr_noneMatch() {
        CompositeOrSearchStrategy composite = new CompositeOrSearchStrategy()
                .add(new NameSearchStrategy("Pizza"))
                .add(new NameSearchStrategy("Taco"));
        assertFalse(composite.matches(safeRecipe));
        assertFalse(composite.matches(glutenRecipe));
        assertFalse(composite.matches(mixedRecipe));
    }
}
