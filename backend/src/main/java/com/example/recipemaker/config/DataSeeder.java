package com.example.recipemaker.config;

import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PatientConditionRepository conditionRepo;
    private final RecipeComponentRepository componentRepo;
    private final RecipeRepository recipeRepo;
    private final AppUserRepository userRepo;
    private final PatientProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return; // Already seeded

        // === Users ===
        AppUser admin = userRepo.save(AppUser.builder()
                .username("admin").password(passwordEncoder.encode("admin")).role("ADMIN").build());

        AppUser doctor = userRepo.save(AppUser.builder()
                .username("doctor1").password(passwordEncoder.encode("doctor")).role("DOCTOR").build());

        AppUser dietician = userRepo.save(AppUser.builder()
                .username("dietician1").password(passwordEncoder.encode("dietician")).role("DIETICIAN").build());

        AppUser patientUser1 = userRepo.save(AppUser.builder()
                .username("patient1").password(passwordEncoder.encode("patient")).role("PATIENT").build());

        AppUser patientUser2 = userRepo.save(AppUser.builder()
                .username("patient2").password(passwordEncoder.encode("patient")).role("PATIENT").build());

        userRepo.save(AppUser.builder()
                .username("kitchen").password(passwordEncoder.encode("kitchen")).role("KITCHEN").build());

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
                .createdByDoctor(doctor)
                .build());

        // === Patient Profiles ===
        PatientProfile profile1 = profileRepo.save(PatientProfile.builder()
                .name("Alice Smith").age(34).notes("Requires low-sodium meals")
                .user(patientUser1).assignedDoctor(doctor)
                .conditions(Set.of(celiac, lactoseIntolerance))
                .build());

        PatientProfile profile2 = profileRepo.save(PatientProfile.builder()
                .name("Bob Jones").age(52).notes("Diabetic patient")
                .user(patientUser2).assignedDoctor(doctor)
                .conditions(Set.of(diabetes))
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

        NonModifiableComponent gardenSalad = new NonModifiableComponent();
        gardenSalad.setName("Garden Salad Base");
        gardenSalad.setDescription("Fresh mixed greens, cucumber, and cherry tomatoes");
        gardenSalad.setIncompatibleConditions(Set.of());
        gardenSalad = componentRepo.save(gardenSalad);

        NonModifiableComponent vegSoup = new NonModifiableComponent();
        vegSoup.setName("Clear Vegetable Soup");
        vegSoup.setDescription("Light broth with seasonal vegetables");
        vegSoup.setIncompatibleConditions(Set.of());
        vegSoup = componentRepo.save(vegSoup);

        NonModifiableComponent yogurtBase = new NonModifiableComponent();
        yogurtBase.setName("Greek Yogurt Base");
        yogurtBase.setDescription("Creamy Greek yogurt");
        yogurtBase.setIncompatibleConditions(Set.of(lactoseIntolerance));
        yogurtBase = componentRepo.save(yogurtBase);

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

        ModifiableComponent lemonDressing = new ModifiableComponent();
        lemonDressing.setName("Lemon Herb Dressing");
        lemonDressing.setDescription("Light lemon and herb vinaigrette");
        lemonDressing.setIncompatibleConditions(Set.of());
        lemonDressing = componentRepo.save(lemonDressing);

        // === Recipes (MAIN meals) ===
        recipeRepo.save(Recipe.builder()
                .name("Grilled Chicken & Broccoli").mealCourse(MealCourse.LUNCH).mealType(MealType.MAIN)
                .description("Lean grilled chicken with steamed broccoli. Safe for most dietary restrictions.")
                .mainComponent(grilledChicken).modifiableComponents(Set.of(steamedBroccoli, tomatoSauce))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Creamy Pasta Alfredo").mealCourse(MealCourse.DINNER).mealType(MealType.MAIN)
                .description("Wheat pasta with cream sauce and parmesan. Contains gluten and dairy.")
                .mainComponent(pastaBase).modifiableComponents(Set.of(creamSauce, cheeseTopping))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Pasta Marinara").mealCourse(MealCourse.DINNER).mealType(MealType.MAIN)
                .description("Wheat pasta with tomato basil sauce. Contains gluten but no dairy.")
                .mainComponent(pastaBase).modifiableComponents(Set.of(tomatoSauce, steamedBroccoli))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Salmon Power Bowl").mealCourse(MealCourse.LUNCH).mealType(MealType.MAIN)
                .description("Baked salmon with rice, honey glaze, and walnut crumble.")
                .mainComponent(salmonFillet).modifiableComponents(Set.of(honeyGlaze, walnutCrumble, steamedBroccoli))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Teriyaki Rice Bowl").mealCourse(MealCourse.LUNCH).mealType(MealType.MAIN)
                .description("Steamed rice with soy sauce and broccoli.")
                .mainComponent(riceBowlBase).modifiableComponents(Set.of(soySauce, steamedBroccoli))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Chicken Comfort Plate").mealCourse(MealCourse.DINNER).mealType(MealType.MAIN)
                .description("Grilled chicken with mashed potatoes and honey glaze.")
                .mainComponent(grilledChicken).modifiableComponents(Set.of(mashedPotato, honeyGlaze))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Classic Pancake Breakfast").mealCourse(MealCourse.BREAKFAST).mealType(MealType.MAIN)
                .description("Buttermilk pancakes with maple syrup and fresh berries.")
                .mainComponent(pancakeBase).modifiableComponents(Set.of(mapleSyrup, freshBerries))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Simple Baked Salmon").mealCourse(MealCourse.DINNER).mealType(MealType.MAIN)
                .description("Baked salmon with steamed broccoli and tomato sauce. Safe for all conditions.")
                .mainComponent(salmonFillet).modifiableComponents(Set.of(steamedBroccoli, tomatoSauce))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Chicken & Rice").mealCourse(MealCourse.DINNER).mealType(MealType.MAIN)
                .description("Simple grilled chicken with steamed rice.")
                .mainComponent(grilledChicken).modifiableComponents(Set.of(soySauce, tomatoSauce))
                .createdBy(dietician).build());

        // === Recipes (SIDE meals) ===
        recipeRepo.save(Recipe.builder()
                .name("Garden Side Salad").mealCourse(MealCourse.LUNCH).mealType(MealType.SIDE)
                .description("Fresh garden salad with lemon herb dressing.")
                .mainComponent(gardenSalad).modifiableComponents(Set.of(lemonDressing))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Garden Salad Dinner Side").mealCourse(MealCourse.DINNER).mealType(MealType.SIDE)
                .description("Fresh garden salad with lemon herb dressing, perfect as a dinner side.")
                .mainComponent(gardenSalad).modifiableComponents(Set.of(lemonDressing))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Vegetable Soup Side").mealCourse(MealCourse.LUNCH).mealType(MealType.SIDE)
                .description("Light clear vegetable soup — warming and safe for all conditions.")
                .mainComponent(vegSoup).modifiableComponents(Set.of())
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Vegetable Soup Dinner Side").mealCourse(MealCourse.DINNER).mealType(MealType.SIDE)
                .description("Light clear vegetable soup for dinner.")
                .mainComponent(vegSoup).modifiableComponents(Set.of())
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Yogurt Berry Parfait").mealCourse(MealCourse.BREAKFAST).mealType(MealType.SIDE)
                .description("Creamy Greek yogurt with fresh mixed berries. Contains dairy.")
                .mainComponent(yogurtBase).modifiableComponents(Set.of(freshBerries, mapleSyrup))
                .createdBy(dietician).build());

        recipeRepo.save(Recipe.builder()
                .name("Steamed Broccoli Side").mealCourse(MealCourse.DINNER).mealType(MealType.SIDE)
                .description("Simple steamed broccoli florets. Safe for all conditions.")
                .mainComponent(gardenSalad).modifiableComponents(Set.of(steamedBroccoli))
                .createdBy(dietician).build());
    }
}
