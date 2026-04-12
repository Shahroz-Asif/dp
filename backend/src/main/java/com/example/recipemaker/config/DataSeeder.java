package com.example.recipemaker.config;

import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PatientConditionRepository conditionRepo;
    private final RecipeComponentRepository componentRepo;
    private final RecipeRepository recipeRepo;

    @Override
    public void run(String... args) {
        if (conditionRepo.count() > 0) return; // Already seeded

        // === Patient Conditions ===
        PatientCondition celiac = conditionRepo.save(PatientCondition.builder()
                .name("Celiac Disease")
                .description("Autoimmune disorder requiring strict gluten-free diet")
                .build());

        PatientCondition lactoseIntolerance = conditionRepo.save(PatientCondition.builder()
                .name("Lactose Intolerance")
                .description("Inability to digest lactose found in dairy products")
                .build());

        PatientCondition diabetes = conditionRepo.save(PatientCondition.builder()
                .name("Type 2 Diabetes")
                .description("Requires low-sugar, low-glycemic diet")
                .build());

        PatientCondition ckd = conditionRepo.save(PatientCondition.builder()
                .name("Chronic Kidney Disease")
                .description("Requires low-sodium, low-potassium, low-phosphorus diet")
                .build());

        PatientCondition nutAllergy = conditionRepo.save(PatientCondition.builder()
                .name("Tree Nut Allergy")
                .description("Severe allergic reaction to tree nuts")
                .build());

        // === Non-Modifiable (Main) Components ===
        NonModifiableComponent grilledChicken = new NonModifiableComponent();
        grilledChicken.setName("Grilled Chicken Breast");
        grilledChicken.setDescription("Lean grilled chicken breast, unseasoned");
        grilledChicken.setIncompatibleConditions(Set.of());
        grilledChicken = componentRepo.save(grilledChicken);

        NonModifiableComponent pastaBase = new NonModifiableComponent();
        pastaBase.setName("Wheat Pasta Base");
        pastaBase.setDescription("Traditional wheat flour pasta");
        pastaBase.setIncompatibleConditions(Set.of(celiac));
        pastaBase = componentRepo.save(pastaBase);

        NonModifiableComponent salmonFillet = new NonModifiableComponent();
        salmonFillet.setName("Baked Salmon Fillet");
        salmonFillet.setDescription("Oven-baked Atlantic salmon");
        salmonFillet.setIncompatibleConditions(Set.of());
        salmonFillet = componentRepo.save(salmonFillet);

        NonModifiableComponent riceBowlBase = new NonModifiableComponent();
        riceBowlBase.setName("Steamed White Rice");
        riceBowlBase.setDescription("Plain steamed white rice");
        riceBowlBase.setIncompatibleConditions(Set.of());
        riceBowlBase = componentRepo.save(riceBowlBase);

        NonModifiableComponent pancakeBase = new NonModifiableComponent();
        pancakeBase.setName("Buttermilk Pancake Stack");
        pancakeBase.setDescription("Classic buttermilk pancakes made with wheat flour and dairy");
        pancakeBase.setIncompatibleConditions(Set.of(celiac, lactoseIntolerance));
        pancakeBase = componentRepo.save(pancakeBase);

        // === Modifiable (Sub) Components ===
        ModifiableComponent creamSauce = new ModifiableComponent();
        creamSauce.setName("Cream Sauce");
        creamSauce.setDescription("Rich dairy-based cream sauce");
        creamSauce.setIncompatibleConditions(Set.of(lactoseIntolerance));
        creamSauce = componentRepo.save(creamSauce);

        ModifiableComponent tomatoSauce = new ModifiableComponent();
        tomatoSauce.setName("Tomato Basil Sauce");
        tomatoSauce.setDescription("Fresh tomato and basil sauce, dairy-free");
        tomatoSauce.setIncompatibleConditions(Set.of());
        tomatoSauce = componentRepo.save(tomatoSauce);

        ModifiableComponent cheeseTopping = new ModifiableComponent();
        cheeseTopping.setName("Parmesan Cheese");
        cheeseTopping.setDescription("Grated parmesan cheese topping");
        cheeseTopping.setIncompatibleConditions(Set.of(lactoseIntolerance));
        cheeseTopping = componentRepo.save(cheeseTopping);

        ModifiableComponent walnutCrumble = new ModifiableComponent();
        walnutCrumble.setName("Walnut Crumble");
        walnutCrumble.setDescription("Crushed walnut topping for texture");
        walnutCrumble.setIncompatibleConditions(Set.of(nutAllergy));
        walnutCrumble = componentRepo.save(walnutCrumble);

        ModifiableComponent honeyGlaze = new ModifiableComponent();
        honeyGlaze.setName("Honey Glaze");
        honeyGlaze.setDescription("Sweet honey glaze drizzle");
        honeyGlaze.setIncompatibleConditions(Set.of(diabetes));
        honeyGlaze = componentRepo.save(honeyGlaze);

        ModifiableComponent steamedBroccoli = new ModifiableComponent();
        steamedBroccoli.setName("Steamed Broccoli");
        steamedBroccoli.setDescription("Lightly steamed broccoli florets");
        steamedBroccoli.setIncompatibleConditions(Set.of());
        steamedBroccoli = componentRepo.save(steamedBroccoli);

        ModifiableComponent mashedPotato = new ModifiableComponent();
        mashedPotato.setName("Mashed Potatoes");
        mashedPotato.setDescription("Creamy mashed potatoes with butter");
        mashedPotato.setIncompatibleConditions(Set.of(lactoseIntolerance, ckd));
        mashedPotato = componentRepo.save(mashedPotato);

        ModifiableComponent soySauce = new ModifiableComponent();
        soySauce.setName("Soy Sauce Drizzle");
        soySauce.setDescription("Low-sodium soy sauce");
        soySauce.setIncompatibleConditions(Set.of(ckd));
        soySauce = componentRepo.save(soySauce);

        ModifiableComponent mapleSyrup = new ModifiableComponent();
        mapleSyrup.setName("Maple Syrup");
        mapleSyrup.setDescription("Pure maple syrup topping");
        mapleSyrup.setIncompatibleConditions(Set.of(diabetes));
        mapleSyrup = componentRepo.save(mapleSyrup);

        ModifiableComponent freshBerries = new ModifiableComponent();
        freshBerries.setName("Fresh Mixed Berries");
        freshBerries.setDescription("Blueberries, strawberries, and raspberries");
        freshBerries.setIncompatibleConditions(Set.of());
        freshBerries = componentRepo.save(freshBerries);

        // === Recipes ===

        // 1. Chicken with sides — fully compatible with all conditions
        recipeRepo.save(Recipe.builder()
                .name("Grilled Chicken & Broccoli")
                .description("Lean grilled chicken with steamed broccoli. Safe for most dietary restrictions.")
                .mainComponent(grilledChicken)
                .modifiableComponents(Set.of(steamedBroccoli, tomatoSauce))
                .build());

        // 2. Pasta — main incompatible with Celiac; cream sauce incompatible with Lactose Intolerance
        recipeRepo.save(Recipe.builder()
                .name("Creamy Pasta Alfredo")
                .description("Wheat pasta with cream sauce and parmesan. Contains gluten and dairy.")
                .mainComponent(pastaBase)
                .modifiableComponents(Set.of(creamSauce, cheeseTopping))
                .build());

        // 3. Pasta with safe sub-components — main still incompatible with Celiac
        recipeRepo.save(Recipe.builder()
                .name("Pasta Marinara")
                .description("Wheat pasta with tomato basil sauce. Contains gluten but no dairy.")
                .mainComponent(pastaBase)
                .modifiableComponents(Set.of(tomatoSauce, steamedBroccoli))
                .build());

        // 4. Salmon bowl — compatible main; mixed sub-components
        recipeRepo.save(Recipe.builder()
                .name("Salmon Power Bowl")
                .description("Baked salmon with rice, honey glaze, and walnut crumble.")
                .mainComponent(salmonFillet)
                .modifiableComponents(Set.of(honeyGlaze, walnutCrumble, steamedBroccoli))
                .build());

        // 5. Rice bowl — compatible main; soy sauce incompatible with CKD
        recipeRepo.save(Recipe.builder()
                .name("Teriyaki Rice Bowl")
                .description("Steamed rice with soy sauce and broccoli.")
                .mainComponent(riceBowlBase)
                .modifiableComponents(Set.of(soySauce, steamedBroccoli))
                .build());

        // 6. Chicken with risky sides — main ok; sides hit multiple conditions
        recipeRepo.save(Recipe.builder()
                .name("Chicken Comfort Plate")
                .description("Grilled chicken with mashed potatoes and honey glaze.")
                .mainComponent(grilledChicken)
                .modifiableComponents(Set.of(mashedPotato, honeyGlaze))
                .build());

        // 7. Pancake breakfast — main incompatible with Celiac + Lactose Intolerance
        recipeRepo.save(Recipe.builder()
                .name("Classic Pancake Breakfast")
                .description("Buttermilk pancakes with maple syrup and fresh berries.")
                .mainComponent(pancakeBase)
                .modifiableComponents(Set.of(mapleSyrup, freshBerries))
                .build());

        // 8. Salmon simple — fully compatible with all conditions
        recipeRepo.save(Recipe.builder()
                .name("Simple Baked Salmon")
                .description("Baked salmon with steamed broccoli and tomato sauce. Safe for all conditions.")
                .mainComponent(salmonFillet)
                .modifiableComponents(Set.of(steamedBroccoli, tomatoSauce))
                .build());
    }
}
