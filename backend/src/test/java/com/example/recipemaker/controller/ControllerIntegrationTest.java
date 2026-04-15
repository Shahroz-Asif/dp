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

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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
}
