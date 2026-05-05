package com.example.recipemaker.controller;

import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RecipeRepository recipeRepo;
    @Autowired RecipeComponentRepository componentRepo;
    @Autowired PatientConditionRepository conditionRepo;
    @Autowired NotificationRepository notificationRepo;

    private Long celiacId;
    private Long lactoseId;

    @BeforeEach
    void setUp() {
        celiacId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Celiac Disease")).findFirst().orElseThrow().getId();
        lactoseId = conditionRepo.findAll().stream()
                .filter(c -> c.getName().equals("Lactose Intolerance")).findFirst().orElseThrow().getId();
    }

    // ── Auth endpoints (public) ──────────────────────────────────────
    @Nested
    class AuthEndpoints {
        @Test
        void registerUser() throws Exception {
            String json = "{\"username\":\"testuser1\",\"password\":\"pass123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("User registered"));
        }
    }

    // ── Recipe endpoints (authenticated) ─────────────────────────────
    @Nested
    @WithMockUser(username = "admin", roles = "ADMIN")
    class RecipeEndpoints {

        @Test
        void listAllRecipes() throws Exception {
            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))))
                    .andExpect(jsonPath("$[0].mainComponent").exists());
        }

        @Test
        void getRecipeById() throws Exception {
            Recipe r = recipeRepo.findAll().stream()
                    .filter(x -> x.getName().equals("Grilled Chicken & Broccoli")).findFirst().orElseThrow();
            mockMvc.perform(get("/api/recipes/" + r.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Grilled Chicken & Broccoli"))
                    .andExpect(jsonPath("$.mainComponent.modifiable").value(false));
        }

        @Test
        void createRecipe() throws Exception {
            RecipeComponent main = componentRepo.findAll().stream()
                    .filter(c -> c.getName().equals("Steamed White Rice")).findFirst().orElseThrow();
            RecipeComponent sub = componentRepo.findAll().stream()
                    .filter(c -> c.getName().equals("Steamed Broccoli")).findFirst().orElseThrow();

            String json = String.format(
                    "{\"name\":\"API Test Bowl\",\"description\":\"Created via API\"," +
                    "\"mainComponentId\":%d,\"modifiableComponentIds\":[%d]}",
                    main.getId(), sub.getId());

            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("API Test Bowl"))
                    .andExpect(jsonPath("$.mainComponent.name").value("Steamed White Rice"));
        }

        @Test
        void deleteRecipe() throws Exception {
            RecipeComponent main = componentRepo.findAll().stream()
                    .filter(c -> c.getName().equals("Baked Salmon Fillet")).findFirst().orElseThrow();
            String createJson = String.format(
                    "{\"name\":\"Delete Me\",\"mainComponentId\":%d}", main.getId());
            String resp = mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON).content(createJson))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long id = objectMapper.readTree(resp).get("id").asLong();

            mockMvc.perform(delete("/api/recipes/" + id))
                    .andExpect(status().isNoContent());

            // After deletion, fetching should throw NoSuchElementException (propagated as ServletException)
            assertThrows(Exception.class, () ->
                    mockMvc.perform(get("/api/recipes/" + id)));
        }

        @Test
        void checkCompatibility() throws Exception {
            Recipe r = recipeRepo.findAll().stream()
                    .filter(x -> x.getName().equals("Creamy Pasta Alfredo")).findFirst().orElseThrow();
            String json = objectMapper.writeValueAsString(Set.of(celiacId));

            mockMvc.perform(post("/api/recipes/" + r.getId() + "/check-compatibility")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recipeSelectable").value(false))
                    .andExpect(jsonPath("$.reason", containsString("Wheat Pasta Base")));
        }

        @Test
        void searchByName() throws Exception {
            mockMvc.perform(get("/api/recipes/search").param("name", "Salmon"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void searchByCompatibility() throws Exception {
            mockMvc.perform(get("/api/recipes/search")
                            .param("compatibleConditionIds", celiacId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", everyItem(
                            not(anyOf(containsString("Pasta"), containsString("Pancake"))))));
        }
    }

    // ── Component endpoints ──────────────────────────────────────────
    @Nested
    @WithMockUser(username = "admin", roles = "ADMIN")
    class ComponentEndpoints {

        @Test
        void listAllComponents() throws Exception {
            mockMvc.perform(get("/api/components"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(15))));
        }

        @Test
        void getComponentById() throws Exception {
            RecipeComponent comp = componentRepo.findAll().stream().findFirst().orElseThrow();
            mockMvc.perform(get("/api/components/" + comp.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(comp.getName()));
        }
    }

    // ── Condition endpoints ──────────────────────────────────────────
    @Nested
    @WithMockUser(username = "admin", roles = "ADMIN")
    class ConditionEndpoints {

        @Test
        void listAllConditions() throws Exception {
            mockMvc.perform(get("/api/conditions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        void getConditionById() throws Exception {
            mockMvc.perform(get("/api/conditions/" + celiacId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Celiac Disease"));
        }

        @Test
        void createAndDeleteCondition() throws Exception {
            String json = "{\"name\":\"Test Allergy\",\"description\":\"For testing\"}";
            String resp = mockMvc.perform(post("/api/conditions")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Test Allergy"))
                    .andReturn().getResponse().getContentAsString();
            Long id = objectMapper.readTree(resp).get("id").asLong();

            mockMvc.perform(delete("/api/conditions/" + id))
                    .andExpect(status().isNoContent());
        }
    }

    // ── Patient profile endpoints ────────────────────────────────────
    @Nested
    @WithMockUser(username = "admin", roles = "ADMIN")
    class PatientEndpoints {

        @Test
        void createAndListProfile() throws Exception {
            String json = "{\"name\":\"John Doe\",\"age\":45,\"notes\":\"Diabetic patient\"}";
            mockMvc.perform(post("/api/patients")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("John Doe"));

            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    // ── Security ─────────────────────────────────────────────────────
    @Nested
    class SecurityTests {

        @Test
        void unauthenticatedAccessBlocked() throws Exception {
            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void authEndpointIsPublic() throws Exception {
            String json = "{\"username\":\"sectest\",\"password\":\"pass123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated());
        }
    }

    // ── Meal Order endpoints (Patient) ────────────────────────────────
    @Nested
    @WithMockUser(username = "patient2", roles = "PATIENT")
    class MealOrderEndpoints {

        private long safeRecipeId() {
            return recipeRepo.findAll().stream()
                    .filter(r -> r.getName().equals("Grilled Chicken & Broccoli"))
                    .findFirst().orElseThrow().getId();
        }

        @Test
        void placeOrderReturnsCreated() throws Exception {
            String json = String.format("{\"recipeId\":%d,\"selectedComponentIds\":[]}", safeRecipeId());
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recipeName").value("Grilled Chicken & Broccoli"))
                    .andExpect(jsonPath("$.status").value("REQUESTED"))
                    .andExpect(jsonPath("$.patientName").value("Bob Jones"));
        }

        @Test
        void activeOrdersEndpointReturnsOk() throws Exception {
            mockMvc.perform(get("/api/orders/active"))
                    .andExpect(status().isOk());
        }

        @Test
        void orderHistoryEndpointReturnsOk() throws Exception {
            mockMvc.perform(get("/api/orders/history"))
                    .andExpect(status().isOk());
        }

        @Test
        void nonPatientCannotPlaceOrder() throws Exception {
            // Simulate a KITCHEN user trying to place an order
            String json = String.format("{\"recipeId\":%d,\"selectedComponentIds\":[]}", safeRecipeId());
            mockMvc.perform(post("/api/orders")
                            .with(httpBasic("kitchen", "kitchen"))
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Kitchen endpoints ─────────────────────────────────────────────
    @Nested
    class KitchenEndpoints {

        @Test
        @WithMockUser(username = "kitchen", roles = "KITCHEN")
        void getActiveOrdersAsKitchen() throws Exception {
            mockMvc.perform(get("/api/kitchen/orders"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getActiveOrdersAsAdmin() throws Exception {
            mockMvc.perform(get("/api/kitchen/orders"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "patient2", roles = "PATIENT")
        void patientCannotAccessKitchenView() throws Exception {
            mockMvc.perform(get("/api/kitchen/orders"))
                    .andExpect(status().isForbidden());
        }

        /**
         * Place an order as patient2 (real HTTP Basic auth so the transaction
         * actually commits and the order is visible to kitchen), then advance it.
         * Uses real credentials so the full request/response cycle exercises
         * SecurityConfig, MealOrderService, and KitchenController together.
         */
        @Test
        void placeOrderThenAdvanceStatusAsKitchen() throws Exception {
            long recipeId = recipeRepo.findAll().stream()
                    .filter(r -> r.getName().equals("Simple Baked Salmon"))
                    .findFirst().orElseThrow().getId();

            // patient2 places an order (transaction commits)
            String placeJson = String.format("{\"recipeId\":%d,\"selectedComponentIds\":[]}", recipeId);
            MvcResult placed = mockMvc.perform(post("/api/orders")
                            .with(httpBasic("patient2", "patient"))
                            .contentType(MediaType.APPLICATION_JSON).content(placeJson))
                    .andExpect(status().isCreated())
                    .andReturn();

            Long orderId = objectMapper.readTree(placed.getResponse().getContentAsString()).get("id").asLong();

            // kitchen advances to PREPARING
            mockMvc.perform(put("/api/kitchen/orders/" + orderId + "/advance")
                            .with(httpBasic("kitchen", "kitchen")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PREPARING"));
        }
    }

    // ── Notification endpoints ────────────────────────────────────────
    @Nested
    class NotificationEndpoints {

        @Test
        @WithMockUser(username = "kitchen", roles = "KITCHEN")
        void getNotificationsReturnsOk() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "patient2", roles = "PATIENT")
        void patientCanFetchTheirNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void unauthenticatedCannotAccessNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "kitchen", roles = "KITCHEN")
        void markAllReadReturnsNoContent() throws Exception {
            mockMvc.perform(put("/api/notifications/read-all"))
                    .andExpect(status().isOk());
        }

        /**
         * End-to-end: patient places an order → kitchen gets a notification →
         * kitchen marks it as read.  This exercises the notification REST
         * endpoints against data produced by the Observer pattern.
         *
         * Uses patient1 (celiac + lactoseIntolerance) with a recipe safe for those
         * conditions so no conflict with orders placed by other tests.
         */
        @Test
        void placeOrderCreatesNotificationForKitchen() throws Exception {
            // Count existing unread for kitchen
            long before = notificationRepo.countByRecipientUsernameAndReadFalse("kitchen");

            // patient1 places a DINNER/MAIN order — "Simple Baked Salmon" is safe for celiac+lactose
            long recipeId = recipeRepo.findAll().stream()
                    .filter(r -> r.getName().equals("Simple Baked Salmon"))
                    .findFirst().orElseThrow().getId();
            mockMvc.perform(post("/api/orders")
                            .with(httpBasic("patient1", "patient"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"recipeId\":%d,\"selectedComponentIds\":[]}", recipeId)))
                    .andExpect(status().isCreated());

            // kitchen queries its notifications and should see at least one more
            MvcResult result = mockMvc.perform(get("/api/notifications")
                            .with(httpBasic("kitchen", "kitchen")))
                    .andExpect(status().isOk())
                    .andReturn();

            long after = notificationRepo.countByRecipientUsernameAndReadFalse("kitchen");
            assertTrue(after > before, "Kitchen must have received at least one new notification");

            // kitchen marks the newest notification as read via PUT /{id}/read
            Long notifId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get(0).get("id").asLong();
            mockMvc.perform(put("/api/notifications/" + notifId + "/read")
                            .with(httpBasic("kitchen", "kitchen")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.read").value(true));
        }
    }
}

