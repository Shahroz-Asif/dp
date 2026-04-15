package com.example.recipemaker.service;

import com.example.recipemaker.config.DataSeeder;
import com.example.recipemaker.dto.CompatibilityResult;
import com.example.recipemaker.dto.ComponentCompatibility;
import com.example.recipemaker.dto.RecipeRequest;
import com.example.recipemaker.dto.RecipeResponse;
import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RecipeServiceIntegrationTest {

    @Autowired RecipeService recipeService;
    @Autowired RecipeRepository recipeRepo;
    @Autowired RecipeComponentRepository componentRepo;
    @Autowired PatientConditionRepository conditionRepo;

    // IDs populated by DataSeeder — resolved in setUp
    private Long celiacId;
    private Long lactoseId;
    private Long diabetesId;
    private Long ckdId;
    private Long nutAllergyId;

    @BeforeEach
    void setUp() {
        // DataSeeder runs on startup; resolve condition IDs by name
        celiacId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Celiac Disease")).findFirst().orElseThrow().getId();
        lactoseId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Lactose Intolerance")).findFirst().orElseThrow().getId();
        diabetesId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Type 2 Diabetes")).findFirst().orElseThrow().getId();
        ckdId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Chronic Kidney Disease")).findFirst().orElseThrow().getId();
        nutAllergyId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Tree Nut Allergy")).findFirst().orElseThrow().getId();
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private Recipe findRecipeByName(String name) {
        return recipeRepo.findAll().stream()
                .filter(r -> r.getName().equals(name)).findFirst().orElseThrow();
    }

    private RecipeComponent findComponentByName(String name) {
        return componentRepo.findAll().stream()
                .filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
    }

    // ── Seeder Verification ──────────────────────────────────────────
    @Nested
    class DataSeederTests {
        @Test
        void seeds5Conditions() {
            assertEquals(5, conditionRepo.count());
        }

        @Test
        void seeds5MainComponents() {
            long mainCount = componentRepo.findAll().stream()
                    .filter(c -> !c.isModifiable()).count();
            assertEquals(8, mainCount);
        }

        @Test
        void seeds10ModifiableComponents() {
            long modCount = componentRepo.findAll().stream()
                    .filter(RecipeComponent::isModifiable).count();
            assertEquals(11, modCount);
        }

        @Test
        void seeds8Recipes() {
            assertTrue(recipeRepo.count() >= 8, "Should have at least 8 seeded recipes");
        }

        @Test
        void everyRecipeHasMainComponent() {
            recipeRepo.findAll().forEach(r ->
                    assertNotNull(r.getMainComponent(), "Recipe '" + r.getName() + "' missing main component"));
        }

        @Test
        void everyRecipeMainComponentIsNonModifiable() {
            recipeRepo.findAll().forEach(r ->
                    assertFalse(r.getMainComponent().isModifiable(),
                            "Main component of '" + r.getName() + "' should be non-modifiable"));
        }

        @Test
        void everyRecipeModifiableComponentsAreModifiable() {
            recipeRepo.findAll().forEach(r ->
                    r.getModifiableComponents().forEach(c ->
                            assertTrue(c.isModifiable(),
                                    "Component '" + c.getName() + "' in recipe '" + r.getName() + "' should be modifiable")));
        }
    }

    // ── Compatibility Checks ─────────────────────────────────────────
    @Nested
    class CompatibilityTests {

        @Test
        void fullyCompatibleRecipe_allConditions() {
            // "Grilled Chicken & Broccoli" — safe main, all safe subs
            Recipe r = findRecipeByName("Grilled Chicken & Broccoli");
            CompatibilityResult result = recipeService.checkCompatibility(
                    r.getId(), Set.of(celiacId, lactoseId, diabetesId, ckdId, nutAllergyId));

            assertTrue(result.isRecipeSelectable(), "Recipe should be selectable for all conditions");
            result.getComponentDetails().forEach(cd ->
                    assertTrue(cd.isSelectable(), "Component '" + cd.getComponentName() + "' should be selectable"));
        }

        @Test
        void celiacBlocksWheatPastaRecipe() {
            // "Creamy Pasta Alfredo" — wheat main → blocked for celiac
            Recipe r = findRecipeByName("Creamy Pasta Alfredo");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of(celiacId));

            assertFalse(result.isRecipeSelectable(), "Recipe should be unselectable for celiac");
            assertTrue(result.getReason().contains("Wheat Pasta Base"));
        }

        @Test
        void celiacBlocksPastaMarinaraToo() {
            // "Pasta Marinara" — same wheat main, even though subs are safe
            Recipe r = findRecipeByName("Pasta Marinara");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of(celiacId));

            assertFalse(result.isRecipeSelectable());
        }

        @Test
        void lactoseBlocksSubcomponentsButNotRecipe() {
            // "Chicken Comfort Plate" — chicken main (safe), mashed potatoes sub (lactose-incompatible)
            Recipe r = findRecipeByName("Chicken Comfort Plate");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of(lactoseId));

            assertTrue(result.isRecipeSelectable(), "Recipe should still be selectable");

            ComponentCompatibility mashedPotato = result.getComponentDetails().stream()
                    .filter(cd -> cd.getComponentName().equals("Mashed Potatoes")).findFirst().orElseThrow();
            assertFalse(mashedPotato.isSelectable(), "Mashed Potatoes should be unselectable for lactose");
            assertTrue(mashedPotato.isModifiable());

            // Honey Glaze should be fine for lactose
            ComponentCompatibility honeyGlaze = result.getComponentDetails().stream()
                    .filter(cd -> cd.getComponentName().equals("Honey Glaze")).findFirst().orElseThrow();
            assertTrue(honeyGlaze.isSelectable());
        }

        @Test
        void diabetesBlocksHoneyGlazeSubcomponent() {
            // "Salmon Power Bowl" — salmon main (safe), honey glaze (diabetes-incompatible)
            Recipe r = findRecipeByName("Salmon Power Bowl");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of(diabetesId));

            assertTrue(result.isRecipeSelectable());

            ComponentCompatibility honey = result.getComponentDetails().stream()
                    .filter(cd -> cd.getComponentName().equals("Honey Glaze")).findFirst().orElseThrow();
            assertFalse(honey.isSelectable());
            assertEquals("Type 2 Diabetes", honey.getIncompatibleCondition());
        }

        @Test
        void nutAllergyBlocksWalnutSubcomponent() {
            Recipe r = findRecipeByName("Salmon Power Bowl");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of(nutAllergyId));

            assertTrue(result.isRecipeSelectable());

            ComponentCompatibility walnut = result.getComponentDetails().stream()
                    .filter(cd -> cd.getComponentName().equals("Walnut Crumble")).findFirst().orElseThrow();
            assertFalse(walnut.isSelectable());
            assertEquals("Tree Nut Allergy", walnut.getIncompatibleCondition());

            // Broccoli should remain selectable
            ComponentCompatibility broccoli = result.getComponentDetails().stream()
                    .filter(cd -> cd.getComponentName().equals("Steamed Broccoli")).findFirst().orElseThrow();
            assertTrue(broccoli.isSelectable());
        }

        @Test
        void ckdBlocksSoySauceInRiceBowl() {
            Recipe r = findRecipeByName("Teriyaki Rice Bowl");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of(ckdId));

            assertTrue(result.isRecipeSelectable(), "Rice main is safe for CKD");

            ComponentCompatibility soy = result.getComponentDetails().stream()
                    .filter(cd -> cd.getComponentName().equals("Soy Sauce Drizzle")).findFirst().orElseThrow();
            assertFalse(soy.isSelectable());
        }

        @Test
        void pancakeBlockedByCeliacAndLactose() {
            // "Classic Pancake Breakfast" — buttermilk pancakes main (celiac + lactose incompatible)
            Recipe r = findRecipeByName("Classic Pancake Breakfast");

            CompatibilityResult celiacResult = recipeService.checkCompatibility(r.getId(), Set.of(celiacId));
            assertFalse(celiacResult.isRecipeSelectable());

            CompatibilityResult lactoseResult = recipeService.checkCompatibility(r.getId(), Set.of(lactoseId));
            assertFalse(lactoseResult.isRecipeSelectable());

            CompatibilityResult bothResult = recipeService.checkCompatibility(
                    r.getId(), Set.of(celiacId, lactoseId));
            assertFalse(bothResult.isRecipeSelectable());
        }

        @Test
        void simpleSalmonSafeForAllConditions() {
            Recipe r = findRecipeByName("Simple Baked Salmon");
            CompatibilityResult result = recipeService.checkCompatibility(
                    r.getId(), Set.of(celiacId, lactoseId, diabetesId, ckdId, nutAllergyId));

            assertTrue(result.isRecipeSelectable());
            result.getComponentDetails().forEach(cd ->
                    assertTrue(cd.isSelectable(), cd.getComponentName() + " should be selectable"));
        }

        @Test
        void noConditions_everythingSelectable() {
            Recipe r = findRecipeByName("Creamy Pasta Alfredo");
            CompatibilityResult result = recipeService.checkCompatibility(r.getId(), Set.of());

            assertTrue(result.isRecipeSelectable());
            result.getComponentDetails().forEach(cd -> assertTrue(cd.isSelectable()));
        }
    }

    // ── Search (Composite Strategy) ──────────────────────────────────
    @Nested
    class SearchTests {

        @Test
        void searchByName_chicken() {
            List<Recipe> results = recipeService.searchRecipes("Chicken", null, null);
            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(r -> r.getName().contains("Chicken")));
        }

        @Test
        void searchByName_pasta() {
            List<Recipe> results = recipeService.searchRecipes("Pasta", null, null);
            assertEquals(2, results.size());
        }

        @Test
        void searchByName_salmon() {
            List<Recipe> results = recipeService.searchRecipes("Salmon", null, null);
            assertEquals(2, results.size());
        }

        @Test
        void searchByName_noMatch() {
            List<Recipe> results = recipeService.searchRecipes("Pizza", null, null);
            assertTrue(results.isEmpty());
        }

        @Test
        void searchByComponentName_creamSauce() {
            List<Recipe> results = recipeService.searchRecipes(null, "Cream Sauce", null);
            assertEquals(1, results.size());
            assertEquals("Creamy Pasta Alfredo", results.get(0).getName());
        }

        @Test
        void searchByComponentName_broccoli() {
            List<Recipe> results = recipeService.searchRecipes(null, "Broccoli", null);
            // Recipes with Steamed Broccoli: Chicken&Broccoli, PastaMarinara, SalmonPowerBowl, TeriyakiRiceBowl, SimpleSalmon
            assertTrue(results.size() >= 4);
        }

        @Test
        void searchCompatibleWithCeliac() {
            // Should exclude recipes whose main component is incompatible with celiac
            List<Recipe> results = recipeService.searchRecipes(null, null, Set.of(celiacId));
            // Excluded: Creamy Pasta Alfredo, Pasta Marinara, Classic Pancake Breakfast (3 blocked)
            assertTrue(results.size() >= 5, "At least 5 recipes should be celiac-compatible");
            assertTrue(results.stream().noneMatch(r -> r.getName().contains("Pasta")));
            assertTrue(results.stream().noneMatch(r -> r.getName().contains("Pancake")));
        }

        @Test
        void searchCompatibleWithLactose() {
            List<Recipe> results = recipeService.searchRecipes(null, null, Set.of(lactoseId));
            // Only Pancake main is lactose-incompatible → excluded
            assertTrue(results.stream().noneMatch(r -> r.getName().contains("Pancake")));
            assertTrue(results.size() >= 7, "At least 7 recipes should be lactose-compatible");
        }

        @Test
        void searchCombined_nameAndCondition() {
            // Looking for "Chicken" recipes safe for celiac
            List<Recipe> results = recipeService.searchRecipes("Chicken", null, Set.of(celiacId));
            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(r -> r.getName().contains("Chicken")));
        }

        @Test
        void searchCombined_nameAndComponent() {
            List<Recipe> results = recipeService.searchRecipes("Salmon", "Honey", null);
            assertEquals(1, results.size());
            assertEquals("Salmon Power Bowl", results.get(0).getName());
        }

        @Test
        void searchCombined_allThreeFilters() {
            // Name contains "Chicken", has "Broccoli" component, compatible with CKD
            List<Recipe> results = recipeService.searchRecipes("Chicken", "Broccoli", Set.of(ckdId));
            assertEquals(1, results.size());
            assertEquals("Grilled Chicken & Broccoli", results.get(0).getName());
        }

        @Test
        void searchNoFilters_returnsAll() {
            List<Recipe> results = recipeService.searchRecipes(null, null, null);
            assertTrue(results.size() >= 8, "Should return at least all 8 seeded recipes");
        }
    }

    // ── Builder via Service (CRUD) ───────────────────────────────────
    @Nested
    class CrudTests {

        @Test
        void createRecipeViaService() {
            RecipeComponent main = findComponentByName("Grilled Chicken Breast");
            RecipeComponent sub = findComponentByName("Tomato Basil Sauce");

            RecipeRequest request = new RecipeRequest();
            request.setName("Custom Chicken Tomato");
            request.setDescription("Custom recipe via builder");
            request.setMainComponentId(main.getId());
            request.setModifiableComponentIds(Set.of(sub.getId()));

            Recipe created = recipeService.createRecipe(request);

            assertNotNull(created.getId());
            assertEquals("Custom Chicken Tomato", created.getName());
            assertEquals(main.getId(), created.getMainComponent().getId());
            assertEquals(1, created.getModifiableComponents().size());
        }

        @Test
        void createRecipeRejectsModifiableAsMain() {
            RecipeComponent modComp = findComponentByName("Cream Sauce");

            RecipeRequest request = new RecipeRequest();
            request.setName("Bad Recipe");
            request.setMainComponentId(modComp.getId());

            assertThrows(IllegalArgumentException.class, () -> recipeService.createRecipe(request));
        }

        @Test
        void createRecipeRejectsNonModifiableAsSub() {
            RecipeComponent main = findComponentByName("Grilled Chicken Breast");
            RecipeComponent nonMod = findComponentByName("Wheat Pasta Base");

            RecipeRequest request = new RecipeRequest();
            request.setName("Bad Recipe");
            request.setMainComponentId(main.getId());
            request.setModifiableComponentIds(Set.of(nonMod.getId()));

            assertThrows(IllegalArgumentException.class, () -> recipeService.createRecipe(request));
        }

        @Test
        void createRecipeFailsOnMissingComponent() {
            RecipeRequest request = new RecipeRequest();
            request.setName("Ghost Recipe");
            request.setMainComponentId(99999L);

            assertThrows(NoSuchElementException.class, () -> recipeService.createRecipe(request));
        }

        @Test
        void updateRecipe() {
            Recipe original = findRecipeByName("Grilled Chicken & Broccoli");
            RecipeComponent newSub = findComponentByName("Tomato Basil Sauce");

            RecipeRequest request = new RecipeRequest();
            request.setName("Updated Chicken Dish");
            request.setDescription("Changed description");
            request.setMainComponentId(original.getMainComponent().getId());
            request.setModifiableComponentIds(Set.of(newSub.getId()));

            Recipe updated = recipeService.updateRecipe(original.getId(), request);
            assertEquals("Updated Chicken Dish", updated.getName());
            assertEquals(1, updated.getModifiableComponents().size());
        }

        @Test
        void deleteRecipe() {
            RecipeComponent main = findComponentByName("Baked Salmon Fillet");
            RecipeComponent sub = findComponentByName("Steamed Broccoli");

            RecipeRequest request = new RecipeRequest();
            request.setName("Temp Recipe");
            request.setMainComponentId(main.getId());
            request.setModifiableComponentIds(Set.of(sub.getId()));

            Recipe created = recipeService.createRecipe(request);
            Long id = created.getId();

            recipeService.deleteRecipe(id);
            assertThrows(NoSuchElementException.class, () -> recipeService.getRecipeById(id));
        }

        @Test
        void getRecipeById_notFound() {
            assertThrows(NoSuchElementException.class, () -> recipeService.getRecipeById(99999L));
        }

        @Test
        void toResponse_mapsCorrectly() {
            Recipe r = findRecipeByName("Salmon Power Bowl");
            RecipeResponse resp = recipeService.toResponse(r);

            assertEquals(r.getId(), resp.getId());
            assertEquals("Salmon Power Bowl", resp.getName());
            assertNotNull(resp.getMainComponent());
            assertFalse(resp.getMainComponent().isModifiable());
            assertEquals(3, resp.getModifiableComponents().size());
            resp.getModifiableComponents().forEach(c -> assertTrue(c.isModifiable()));
        }
    }
}
