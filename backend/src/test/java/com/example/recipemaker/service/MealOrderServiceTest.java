package com.example.recipemaker.service;

import com.example.recipemaker.dto.MealOrderRequest;
import com.example.recipemaker.dto.MealOrderResponse;
import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MealOrderService.
 *
 * Each test creates a fresh AppUser + PatientProfile within the test's
 * @Transactional boundary so they are rolled back after the test and can
 * never conflict with orders committed by ControllerIntegrationTest.
 *
 * Design patterns exercised:
 *   Builder   — MealOrder.builder(), RecipeBuilder, PatientProfile.builder()
 *   Strategy  — recipe search used indirectly via RecipeService
 *   Observer  — NotificationService triggered after commits (not tested here;
 *               see NotificationServiceTest for direct observer method tests)
 */
@SpringBootTest
@Transactional
class MealOrderServiceTest {

    @Autowired MealOrderService mealOrderService;
    @Autowired RecipeRepository recipeRepo;
    @Autowired RecipeComponentRepository componentRepo;
    @Autowired PatientConditionRepository conditionRepo;
    @Autowired AppUserRepository userRepo;
    @Autowired PatientProfileRepository profileRepo;
    @Autowired PasswordEncoder passwordEncoder;

    // Created fresh per-test and rolled back by @Transactional
    private String testUsername;

    private Recipe grilledChickenLunch;  // LUNCH / MAIN
    private Recipe simpleSalmonDinner;   // DINNER / MAIN
    private RecipeComponent steamedBroccoli;

    @BeforeEach
    void setUp() {
        grilledChickenLunch = findRecipe("Grilled Chicken & Broccoli");
        simpleSalmonDinner  = findRecipe("Simple Baked Salmon");
        steamedBroccoli     = findComponent("Steamed Broccoli");

        // Unique username per test — rolled back with the rest of the transaction
        testUsername = "testpatient_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AppUser user = userRepo.save(AppUser.builder()
                .username(testUsername)
                .password(passwordEncoder.encode("pass"))
                .role("PATIENT")
                .build());
        profileRepo.save(PatientProfile.builder()
                .name("Test Patient")
                .age(30)
                .user(user)
                .conditions(Set.of()) // no conditions → all recipes safe
                .build());
        loginAs(testUsername, "PATIENT");
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ── helpers ──────────────────────────────────────────────────────

    private Recipe findRecipe(String name) {
        return recipeRepo.findAll().stream()
                .filter(r -> r.getName().equals(name)).findFirst().orElseThrow();
    }

    private RecipeComponent findComponent(String name) {
        return componentRepo.findAll().stream()
                .filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
    }

    private void loginAs(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private MealOrderRequest req(Recipe recipe) {
        MealOrderRequest r = new MealOrderRequest();
        r.setRecipeId(recipe.getId());
        r.setSelectedComponentIds(Collections.emptyList());
        return r;
    }

    // ── PlaceOrder ───────────────────────────────────────────────────

    @Nested
    class PlaceOrderTests {

        @Test
        void placesOrderSuccessfully() {
            MealOrderResponse resp = mealOrderService.placeOrder(req(grilledChickenLunch));

            assertNotNull(resp.getId());
            assertEquals("Grilled Chicken & Broccoli", resp.getRecipeName());
            assertEquals(OrderStatus.REQUESTED, resp.getStatus());
            assertNotNull(resp.getOrderDate());
            assertEquals("Test Patient", resp.getPatientName());
        }

        @Test
        void placesOrderWithSelectedComponents() {
            MealOrderRequest request = new MealOrderRequest();
            request.setRecipeId(grilledChickenLunch.getId());
            request.setSelectedComponentIds(List.of(steamedBroccoli.getId()));

            MealOrderResponse resp = mealOrderService.placeOrder(request);

            assertEquals(1, resp.getSelectedComponents().size());
            assertEquals("Steamed Broccoli", resp.getSelectedComponents().get(0).getName());
        }

        @Test
        void duplicateOrderSameDayAndMealTypeCourseThrowsConflict() {
            mealOrderService.placeOrder(req(grilledChickenLunch)); // first succeeds

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> mealOrderService.placeOrder(req(grilledChickenLunch)));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertTrue(ex.getReason().toLowerCase().contains("already ordered"));
        }

        @Test
        void canOrderDifferentMealCourseOnSameDay() {
            mealOrderService.placeOrder(req(grilledChickenLunch)); // LUNCH/MAIN
            // DINNER/MAIN — different course, no conflict
            assertDoesNotThrow(() -> mealOrderService.placeOrder(req(simpleSalmonDinner)));
        }

        @Test
        void componentFromDifferentRecipeRejected() {
            // soySauce belongs to "Teriyaki Rice Bowl", not "Grilled Chicken & Broccoli"
            RecipeComponent soySauce = findComponent("Soy Sauce Drizzle");

            MealOrderRequest request = new MealOrderRequest();
            request.setRecipeId(grilledChickenLunch.getId());
            request.setSelectedComponentIds(List.of(soySauce.getId()));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> mealOrderService.placeOrder(request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void nonModifiableComponentRejected() {
            // "Grilled Chicken Breast" is the main component (non-modifiable)
            RecipeComponent mainComp = findComponent("Grilled Chicken Breast");

            MealOrderRequest request = new MealOrderRequest();
            request.setRecipeId(grilledChickenLunch.getId());
            request.setSelectedComponentIds(List.of(mainComp.getId()));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> mealOrderService.placeOrder(request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void incompatibleComponentBlockedByPatientCondition() {
            // Give our test user lactose intolerance, then try to order Mashed Potatoes
            PatientCondition lactose = conditionRepo.findAll().stream()
                    .filter(c -> c.getName().equals("Lactose Intolerance")).findFirst().orElseThrow();

            AppUser user2 = userRepo.save(AppUser.builder()
                    .username(testUsername + "_lactose")
                    .password(passwordEncoder.encode("pass"))
                    .role("PATIENT").build());
            profileRepo.save(PatientProfile.builder()
                    .name("Lactose Patient")
                    .age(40)
                    .user(user2)
                    .conditions(Set.of(lactose))
                    .build());
            loginAs(user2.getUsername(), "PATIENT");

            Recipe chickenComfort = findRecipe("Chicken Comfort Plate");
            RecipeComponent mashedPotato = findComponent("Mashed Potatoes");

            MealOrderRequest request = new MealOrderRequest();
            request.setRecipeId(chickenComfort.getId());
            request.setSelectedComponentIds(List.of(mashedPotato.getId()));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> mealOrderService.placeOrder(request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Mashed Potatoes"));
        }

        @Test
        void userWithNoPatientProfileThrowsBadRequest() {
            // admin user exists but has no patient profile
            loginAs("admin", "ADMIN");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> mealOrderService.placeOrder(req(grilledChickenLunch)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void nonExistentRecipeThrowsNoSuchElement() {
            MealOrderRequest request = new MealOrderRequest();
            request.setRecipeId(99999L);
            request.setSelectedComponentIds(Collections.emptyList());

            assertThrows(NoSuchElementException.class, () -> mealOrderService.placeOrder(request));
        }
    }

    // ── AdvanceOrderStatus ───────────────────────────────────────────

    @Nested
    class AdvanceOrderTests {

        @Test
        void advancesOrderThroughFullLifecycle() {
            MealOrderResponse placed = mealOrderService.placeOrder(req(grilledChickenLunch));
            assertEquals(OrderStatus.REQUESTED, placed.getStatus());

            MealOrderResponse preparing = mealOrderService.advanceOrderStatus(placed.getId());
            assertEquals(OrderStatus.PREPARING, preparing.getStatus());

            MealOrderResponse ready = mealOrderService.advanceOrderStatus(placed.getId());
            assertEquals(OrderStatus.READY, ready.getStatus());

            MealOrderResponse done = mealOrderService.advanceOrderStatus(placed.getId());
            assertEquals(OrderStatus.DONE, done.getStatus());
        }

        @Test
        void cannotAdvanceBeyondDone() {
            Long id = mealOrderService.placeOrder(req(grilledChickenLunch)).getId();
            mealOrderService.advanceOrderStatus(id); // PREPARING
            mealOrderService.advanceOrderStatus(id); // READY
            mealOrderService.advanceOrderStatus(id); // DONE

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> mealOrderService.advanceOrderStatus(id));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void advancingNonExistentOrderThrowsNoSuchElement() {
            assertThrows(NoSuchElementException.class,
                    () -> mealOrderService.advanceOrderStatus(99999L));
        }
    }

    // ── Query methods ────────────────────────────────────────────────

    @Nested
    class QueryTests {

        @Test
        void activeOrdersContainsRequestedOrder() {
            MealOrderResponse placed = mealOrderService.placeOrder(req(grilledChickenLunch));

            List<MealOrderResponse> active = mealOrderService.getActiveOrdersForCurrentPatient();
            assertTrue(active.stream().anyMatch(o -> o.getId().equals(placed.getId())),
                    "REQUESTED order must appear in active");
        }

        @Test
        void doneOrderMovesToHistory() {
            Long id = mealOrderService.placeOrder(req(grilledChickenLunch)).getId();
            mealOrderService.advanceOrderStatus(id); // PREPARING
            mealOrderService.advanceOrderStatus(id); // READY
            mealOrderService.advanceOrderStatus(id); // DONE

            List<MealOrderResponse> active = mealOrderService.getActiveOrdersForCurrentPatient();
            assertTrue(active.stream().noneMatch(o -> o.getId().equals(id)),
                    "DONE order must not appear in active");

            List<MealOrderResponse> history = mealOrderService.getOrderHistoryForCurrentPatient();
            assertTrue(history.stream().anyMatch(o -> o.getId().equals(id)),
                    "DONE order must appear in history");
        }

        @Test
        void getAllActiveOrdersSeesEveryPatient() {
            MealOrderResponse placed = mealOrderService.placeOrder(req(grilledChickenLunch));

            List<MealOrderResponse> all = mealOrderService.getAllActiveOrders();
            assertTrue(all.stream().anyMatch(o -> o.getId().equals(placed.getId())),
                    "Kitchen view must include the newly placed order");
        }
    }
}

