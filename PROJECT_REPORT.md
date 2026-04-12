# RecipeMaker — Patient Recipe System: Project Report

## Table of Contents

1. [Project Overview](#project-overview)
2. [Core Technologies](#core-technologies)
3. [System Architecture](#system-architecture)
4. [Backend](#backend)
   - [Data Model](#data-model)
   - [API Endpoints](#api-endpoints)
   - [Security](#security)
   - [Design Patterns](#design-patterns)
   - [Business Logic](#business-logic)
5. [Frontend](#frontend)
   - [Technology Stack](#frontend-technology-stack)
   - [Application Structure](#application-structure)
   - [Pages & Features](#pages--features)
   - [API Integration](#api-integration)
6. [Testing](#testing)
7. [Developer Experience](#developer-experience)

---

## Project Overview

**RecipeMaker** is a full-stack web application designed for healthcare practitioners to manage and evaluate dietary recipes against patient conditions. The system allows clinicians to browse recipes, compose custom ones from a library of components, and instantly check whether a given recipe or any of its optional sub-components are contraindicated for a patient's registered conditions.

| Property       | Value                              |
|----------------|------------------------------------|
| Version        | `0.0.1-SNAPSHOT`                   |
| Backend        | Spring Boot 3.2.5 / Java 21        |
| Frontend       | React 18 / TypeScript 5            |
| Database       | H2 (in-memory, dev/test)           |
| Auth           | HTTP Basic (bcrypt-hashed)         |
| API Style      | RESTful JSON                       |
| API Docs       | OpenAPI 3 / Swagger UI             |

---

## Core Technologies

### Backend

| Technology                       | Role                                              |
|----------------------------------|---------------------------------------------------|
| **Java 21**                      | Language runtime; virtual threads ready           |
| **Spring Boot 3.2.5**            | Application framework and auto-configuration      |
| **Spring Web MVC**               | REST controller layer                             |
| **Spring Data JPA + Hibernate**  | ORM, repository abstraction, DDL management       |
| **Spring Security**              | Authentication, authorization, password encoding  |
| **H2 Database**                  | Embedded in-memory relational database            |
| **Lombok**                       | Boilerplate reduction (`@Data`, `@Builder`, etc.) |
| **JJWT 0.11.5**                  | JWT library (provisioned for token upgrade path)  |
| **SpringDoc OpenAPI 2.5.0**      | Auto-generated Swagger UI at `/swagger-ui.html`   |
| **Maven**                        | Build tool and dependency management              |

### Frontend

| Technology              | Role                                                      |
|-------------------------|-----------------------------------------------------------|
| **React 18**            | Component-based UI library with concurrent rendering      |
| **TypeScript 5**        | Static typing across the entire frontend codebase         |
| **Vite**                | Build tool and dev server (HMR, fast cold starts)         |
| **React Router v6**     | Client-side routing with nested route layouts             |
| **Axios**               | HTTP client for REST API communication                    |
| **TanStack Query v5**   | Server-state management, caching, and background syncing  |
| **Tailwind CSS v3**     | Utility-first styling with a consistent design system     |
| **shadcn/ui**           | Accessible, composable UI component primitives            |
| **React Hook Form**     | Performant form management with minimal re-renders        |
| **Zod**                 | Runtime schema validation for form inputs and API shapes  |

---

## System Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Browser / Client                 │
│                                                     │
│   React 18 + TypeScript   (Vite dev server :5173)   │
│   ┌──────────┐  ┌────────────┐  ┌───────────────┐  │
│   │  Pages   │  │ Components │  │  API Layer    │  │
│   │ (Router) │  │ (shadcn/ui)│  │  (Axios +     │  │
│   └──────────┘  └────────────┘  │  TanStack Q.) │  │
│                                 └───────┬───────┘  │
└─────────────────────────────────────────┼───────────┘
                          HTTP Basic Auth │ JSON REST
┌─────────────────────────────────────────┼───────────┐
│                Spring Boot :8080        │           │
│   ┌──────────────────────────┐          │           │
│   │  Spring Security Filter  │◄─────────┘           │
│   └────────────┬─────────────┘                      │
│                │                                     │
│   ┌────────────▼─────────────┐                      │
│   │  REST Controllers        │                      │
│   │  (Recipe / Component /   │                      │
│   │   Condition / Patient)   │                      │
│   └────────────┬─────────────┘                      │
│                │                                     │
│   ┌────────────▼─────────────┐                      │
│   │  Service Layer           │                      │
│   │  (RecipeService, etc.)   │                      │
│   └────────────┬─────────────┘                      │
│                │                                     │
│   ┌────────────▼─────────────┐                      │
│   │  JPA Repositories        │                      │
│   └────────────┬─────────────┘                      │
│                │                                     │
│   ┌────────────▼─────────────┐                      │
│   │  H2 In-Memory Database   │                      │
│   └──────────────────────────┘                      │
└─────────────────────────────────────────────────────┘
```

---

## Backend

### Data Model

The backend manages five core entity types related by JPA associations.

```
AppUser ─────────────── PatientProfile
  │ (OneToOne)                │
  │                           │ (ManyToMany via patient_condition)
  │                     PatientCondition
  │                           │
  │                    (ManyToMany via component_incompatibility)
  │                           │
  │                     RecipeComponent (abstract, SINGLE_TABLE)
  │                      ├── NonModifiableComponent  [discriminator: NON_MODIFIABLE]
  │                      └── ModifiableComponent     [discriminator: MODIFIABLE]
  │                           │
  └──────────────────── Recipe
                          ├── mainComponent     (ManyToOne → NonModifiable)
                          └── modifiableComponents (ManyToMany → Modifiable)
```

#### Entity Summary

| Entity                  | Key Fields                                                       | Notes                                          |
|-------------------------|------------------------------------------------------------------|------------------------------------------------|
| `AppUser`               | `id`, `username` (unique), `password`, `role`                    | Password stored as BCrypt hash                 |
| `PatientProfile`        | `id`, `name`, `age`, `notes`, `user`                             | OneToOne with AppUser                          |
| `PatientCondition`      | `id`, `name`, `description`                                      | e.g. Celiac Disease, Lactose Intolerance       |
| `RecipeComponent`       | `id`, `name`, `description`, `incompatibleConditions`            | Abstract; single-table inheritance             |
| `ModifiableComponent`   | (inherits RecipeComponent)                                       | Optional sub-components in a recipe            |
| `NonModifiableComponent`| (inherits RecipeComponent)                                       | Required main component of a recipe            |
| `Recipe`                | `id`, `name`, `description`, `mainComponent`, `modifiableComponents` | Built via `RecipeBuilder`                  |

### API Endpoints

All endpoints require HTTP Basic authentication unless marked **Public**.

#### Authentication — `/api/auth`

| Method | Path                  | Auth   | Description                            |
|--------|-----------------------|--------|----------------------------------------|
| `POST` | `/api/auth/register`  | Public | Register a new user (role: `USER`)     |

#### Recipes — `/api/recipes`

| Method   | Path                              | Description                                            |
|----------|-----------------------------------|--------------------------------------------------------|
| `GET`    | `/api/recipes`                    | List all recipes as `RecipeResponse` DTOs              |
| `GET`    | `/api/recipes/{id}`               | Get single recipe by ID                                |
| `POST`   | `/api/recipes`                    | Create recipe via Builder pattern                      |
| `PUT`    | `/api/recipes/{id}`               | Update recipe fields and components                    |
| `DELETE` | `/api/recipes/{id}`               | Delete recipe (204 No Content)                         |
| `POST`   | `/api/recipes/{id}/check-compatibility` | Check compatibility against a set of condition IDs |
| `GET`    | `/api/recipes/search`             | Search by `name`, `componentName`, `compatibleConditionIds` |
| `GET`    | `/api/recipes/export`             | Export all recipes as JSON                             |
| `POST`   | `/api/recipes/export`             | Bulk import recipes                                    |
| `GET`    | `/api/recipes/history/{id}`       | Recipe change history                                  |

#### Components — `/api/components`

| Method   | Path                    | Description                  |
|----------|-------------------------|------------------------------|
| `GET`    | `/api/components`       | List all recipe components   |
| `GET`    | `/api/components/{id}`  | Get component by ID          |
| `POST`   | `/api/components`       | Create component             |
| `PUT`    | `/api/components/{id}`  | Update component             |
| `DELETE` | `/api/components/{id}`  | Delete component             |

#### Conditions — `/api/conditions`

| Method   | Path                    | Description                    |
|----------|-------------------------|--------------------------------|
| `GET`    | `/api/conditions`       | List all patient conditions    |
| `GET`    | `/api/conditions/{id}`  | Get condition by ID            |
| `POST`   | `/api/conditions`       | Create condition               |
| `PUT`    | `/api/conditions/{id}`  | Update condition               |
| `DELETE` | `/api/conditions/{id}`  | Delete condition               |

#### Patient Profiles — `/api/patients`

| Method   | Path                  | Description               |
|----------|-----------------------|---------------------------|
| `GET`    | `/api/patients`       | List all patient profiles |
| `GET`    | `/api/patients/{id}`  | Get profile by ID         |
| `POST`   | `/api/patients`       | Create profile            |
| `PUT`    | `/api/patients/{id}`  | Update profile            |
| `DELETE` | `/api/patients/{id}`  | Delete profile            |

### Security

- **Mechanism**: HTTP Basic Authentication over every secured route
- **Password storage**: BCrypt via Spring Security's `BCryptPasswordEncoder`
- **Public routes**: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`
- **CSRF**: Disabled (stateless REST API)
- **JWT provisioned**: `jjwt-api/impl/jackson 0.11.5` are present on the classpath; a filter-chain upgrade to Bearer token auth is the natural next step
- **User storage**: `AppUser` entity in H2, loaded via `UserDetailsServiceImpl`

### Design Patterns

#### Builder Pattern — `RecipeBuilder`

A custom fluent builder for `Recipe` that enforces domain invariants at construction time:

```java
Recipe recipe = new RecipeBuilder()
    .name("Gluten-Free Pasta")
    .description("Safe for celiac patients")
    .mainComponent(pastaBase)           // must be NonModifiable or throws
    .addModifiableComponent(oliveSauce) // must be Modifiable or throws
    .build();                           // validates name + mainComponent
```

| Validation rule                          | Exception thrown            |
|------------------------------------------|-----------------------------|
| `mainComponent()` receives a modifiable  | `IllegalArgumentException`  |
| `addModifiableComponent()` receives non-modifiable | `IllegalArgumentException` |
| `build()` called without a name          | `IllegalStateException`     |
| `build()` called without a main component | `IllegalStateException`    |

#### Strategy Pattern (Composite) — `search/`

Recipe searching is implemented as a composable strategy chain:

```
RecipeSearchStrategy (interface: boolean matches(Recipe))
│
├── NameSearchStrategy            – substring match on recipe name
├── ComponentNameSearchStrategy   – match on any component name
├── ConditionCompatibilityStrategy – main component has no forbidden conditions
│
├── CompositeAndSearchStrategy    – ALL strategies must match (short-circuit AND)
└── CompositeOrSearchStrategy     – ANY strategy must match (short-circuit OR)
```

`RecipeService.searchRecipes()` builds a `CompositeAndSearchStrategy` from whichever filter parameters are provided, then applies it to the full recipe list.

#### Repository Pattern

Five `JpaRepository` interfaces (`RecipeRepository`, `RecipeComponentRepository`, `PatientConditionRepository`, `PatientProfileRepository`, `AppUserRepository`) fully abstract all data access, leaving the service layer decoupled from persistence details.

#### Single-Table Inheritance (JPA)

`RecipeComponent` uses `InheritanceType.SINGLE_TABLE` with a `component_type` discriminator column, enabling polymorphic Spring Data JPA queries over both `ModifiableComponent` and `NonModifiableComponent` from a single repository.

### Business Logic

**Compatibility check** (`RecipeService.checkCompatibility`):

| Component type       | Has incompatible condition?   | Effect                                  |
|----------------------|-------------------------------|-----------------------------------------|
| Main component       | Yes                           | Recipe `selectable = false`             |
| Modifiable component | Yes                           | Component `selectable = false`; recipe remains selectable |
| Any component        | No                            | Component `selectable = true`           |

`CompatibilityResult` returns the overall recipe selectability, a human-readable reason, and a per-component `ComponentCompatibility` breakdown.

**Data seeding** (`DataSeeder`):

On first startup, 5 patient conditions, 15 recipe components, and 8 recipes are seeded to enable immediate exploration. The seeder is idempotent (guards on `conditionRepo.count() > 0`).

---

## Frontend

### Frontend Technology Stack

The frontend is a single-page application built with **React 18** and **TypeScript 5**, bundled by **Vite** for near-instant hot-module replacement during development.

| Layer              | Library / Tool         | Version  | Purpose                                          |
|--------------------|------------------------|----------|--------------------------------------------------|
| UI Framework       | React                  | 18.x     | Component tree, concurrent rendering, Suspense   |
| Language           | TypeScript             | 5.x      | End-to-end static typing                         |
| Build Tool         | Vite                   | 5.x      | Dev server (HMR), production bundling (Rollup)   |
| Routing            | React Router           | 6.x      | Declarative nested routes, `<Outlet>` layouts    |
| HTTP Client        | Axios                  | 1.x      | Interceptors for Basic Auth header injection      |
| Server State       | TanStack Query         | 5.x      | Query caching, invalidation, loading/error states|
| Styling            | Tailwind CSS           | 3.x      | Utility-first CSS; purged for production         |
| Component Library  | shadcn/ui              | latest   | Radix-UI-based accessible primitives (Dialog, Table, Badge, etc.) |
| Forms              | React Hook Form        | 7.x      | Uncontrolled form fields, minimal re-renders     |
| Validation         | Zod                    | 3.x      | Schema validation for forms and API response shapes |
| Icons              | Lucide React           | latest   | Consistent SVG icon set                          |

### Application Structure

```
frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── axiosClient.ts         # Axios instance; injects Basic Auth header
│   │   ├── recipes.ts             # Recipe CRUD + search + compatibility calls
│   │   ├── components.ts          # Component CRUD calls
│   │   ├── conditions.ts          # Condition CRUD calls
│   │   └── patients.ts            # Patient profile CRUD calls
│   ├── hooks/
│   │   ├── useRecipes.ts          # TanStack Query hooks for recipe endpoints
│   │   ├── useComponents.ts
│   │   ├── useConditions.ts
│   │   └── usePatients.ts
│   ├── pages/
│   │   ├── LoginPage.tsx          # HTTP Basic credential collection
│   │   ├── RecipeListPage.tsx     # Paginated recipe browser with search bar
│   │   ├── RecipeDetailPage.tsx   # Recipe view + compatibility checker
│   │   ├── RecipeFormPage.tsx     # Create / edit recipe form
│   │   ├── ComponentsPage.tsx     # Component management table
│   │   ├── ConditionsPage.tsx     # Condition management table
│   │   └── PatientsPage.tsx       # Patient profile management
│   ├── components/
│   │   ├── layout/
│   │   │   ├── AppShell.tsx       # Sidebar nav + topbar wrapper
│   │   │   └── PrivateRoute.tsx   # Redirects to /login if unauthenticated
│   │   ├── recipes/
│   │   │   ├── RecipeCard.tsx     # Summary card used in list view
│   │   │   ├── RecipeSearchBar.tsx  # Name / component / condition filters
│   │   │   └── CompatibilityPanel.tsx  # Condition multi-select + result display
│   │   ├── shared/
│   │   │   ├── DataTable.tsx      # Generic sortable table built on shadcn/ui
│   │   │   ├── ConfirmDialog.tsx  # Delete confirmation modal
│   │   │   └── StatusBadge.tsx    # Selectable / Incompatible / Unknown badges
│   │   └── forms/
│   │       ├── RecipeForm.tsx     # Zod-validated form for Recipe
│   │       ├── ComponentForm.tsx
│   │       ├── ConditionForm.tsx
│   │       └── PatientForm.tsx
│   ├── context/
│   │   └── AuthContext.tsx        # Stores credentials; provides useAuth() hook
│   ├── types/
│   │   └── api.ts                 # TypeScript interfaces mirroring backend DTOs
│   ├── App.tsx                    # Route definitions
│   └── main.tsx                   # React DOM entry, QueryClientProvider
├── index.html
├── vite.config.ts                 # Proxy /api → http://localhost:8080
├── tailwind.config.ts
└── tsconfig.json
```

### Pages & Features

#### Login Page (`/login`)

- Accepts `username` and `password` via React Hook Form controlled by Zod schema
- On submit, stores credentials in `AuthContext` (in-memory; no localStorage persistence of raw passwords)
- Performs a test `GET /api/recipes` call to validate credentials before redirecting
- Protected pages redirect here via `PrivateRoute` when credentials are absent

#### Recipe List Page (`/recipes`)

- Fetches all recipes via `useRecipes()` (TanStack Query, stale-while-revalidate)
- **Search bar** with three optional fields that call `GET /api/recipes/search`:
  - Recipe name keyword
  - Component name keyword
  - Condition multi-select (all loaded conditions from `useConditions()`)
- Results rendered as `RecipeCard` grid with recipe name, description, component counts, and a "View" link
- "New Recipe" button navigates to the create form
- Empty state with instructional illustration when no results match

#### Recipe Detail Page (`/recipes/:id`)

- Fetches recipe with `GET /api/recipes/{id}`
- Displays main component (highlighted) and all modifiable components as a list with badges indicating their modifiable status
- **Compatibility Panel**: multi-select of available conditions; on submit calls `POST /api/recipes/{id}/check-compatibility`
  - Overall recipe selectability shown with a prominent colored banner
  - Per-component compatibility result rendered in an expandable table
  - Incompatible conditions named explicitly in the reason string
- Edit / Delete action buttons; Delete triggers `ConfirmDialog`

#### Recipe Form Page (`/recipes/new`, `/recipes/:id/edit`)

- Unified create/edit form using React Hook Form + Zod
- `mainComponentId`: single-select dropdown filtered to **NonModifiable** components only (client filters `isModifiable === false` from `GET /api/components`)
- `modifiableComponentIds`: multi-select filtered to **Modifiable** components only
- On save: `POST /api/recipes` (create) or `PUT /api/recipes/{id}` (update); TanStack Query cache invalidated on success
- Validation errors displayed inline below each field

#### Components Page (`/components`)

- `DataTable` of all recipe components with columns: name, description, type badge (Modifiable / Non-Modifiable), incompatible conditions
- Inline "Add Component" button opens a Dialog containing `ComponentForm`
- Row-level Edit and Delete actions; Delete guarded by `ConfirmDialog`

#### Conditions Page (`/conditions`)

- `DataTable` of all patient conditions
- Inline "Add Condition" button
- Row-level Edit / Delete with confirmation

#### Patients Page (`/patients`)

- `DataTable` of all patient profiles with: name, age, notes
- Inline "Add Patient" button opens `PatientForm` Dialog
- Row-level Edit / Delete

### API Integration

**`axiosClient.ts`** creates a pre-configured Axios instance:

```typescript
import axios from 'axios';

const apiClient = axios.create({ baseURL: '/api' });

apiClient.interceptors.request.use((config) => {
  const { username, password } = getCredentials();   // from AuthContext
  if (username && password) {
    config.headers['Authorization'] =
      'Basic ' + btoa(`${username}:${password}`);
  }
  return config;
});

export default apiClient;
```

Vite's dev server proxies `/api/*` to `http://localhost:8080/api/*`, eliminating CORS issues during development.

**TanStack Query** wraps every API call:

```typescript
// useRecipes.ts
export const useRecipes = () =>
  useQuery({ queryKey: ['recipes'], queryFn: () => apiClient.get('/recipes').then(r => r.data) });

export const useCreateRecipe = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: RecipeRequest) => apiClient.post('/recipes', body).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['recipes'] }),
  });
};
```

**TypeScript DTO mirrors** (`types/api.ts`) align exactly with the backend response shapes:

```typescript
export interface RecipeResponse {
  id: number;
  name: string;
  description: string;
  mainComponent: ComponentResponse;
  modifiableComponents: ComponentResponse[];
}

export interface CompatibilityResult {
  recipeId: number;
  recipeName: string;
  recipeSelectable: boolean;
  reason: string;
  componentDetails: ComponentCompatibility[];
}
```

---

## Testing

### Backend Test Suite

| Test Class                        | Type                          | Tests | Coverage area                                               |
|-----------------------------------|-------------------------------|-------|-------------------------------------------------------------|
| `RecipeBuilderTest`               | Unit (no Spring context)      | 9     | `RecipeBuilder` validation rules and fluent API             |
| `SearchStrategyTest`              | Unit (no Spring context)      | ~18   | All 5 search strategy classes, AND/OR composite logic       |
| `RecipeServiceIntegrationTest`    | `@SpringBootTest @Transactional` | ~35 | Data seeder, CRUD, compatibility rules, search combinations |
| `ControllerIntegrationTest`       | `@SpringBootTest @AutoConfigureMockMvc` | ~20 | All REST endpoints; auth enforcement; 401 on unauthenticated |

### Frontend Test Strategy

| Layer        | Tool                          | Coverage                                                  |
|--------------|-------------------------------|-----------------------------------------------------------|
| Unit         | Vitest + React Testing Library | Page components, hooks, utility functions                 |
| Integration  | Vitest + MSW (Mock Service Worker) | API hook behaviour with mocked HTTP responses         |
| E2E          | Playwright                    | Login → browse recipes → check compatibility flow          |

---

## Developer Experience

### Running the Backend

```bash
# Prerequisites: Java 21, Maven 3.9+
cd recipe_maker
./mvnw spring-boot:run
# API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:recipemaker)
```

### Running the Frontend

```bash
cd recipe_maker/frontend
npm install
npm run dev
# App: http://localhost:5173
```

> The Vite dev server proxies `/api` to `localhost:8080` — no CORS configuration required.

### Default Seeded Credentials

Register a user via `POST /api/auth/register` or Swagger UI to log in. The `DataSeeder` does not create `AppUser` entries automatically.

### API Documentation

Live Swagger UI at `http://localhost:8080/swagger-ui.html` documents all endpoints with request/response schemas, powered by SpringDoc OpenAPI 2.5.0.

---

## Provisioned Upgrade Paths

| Area               | Current                        | Natural Next Step                              |
|--------------------|--------------------------------|------------------------------------------------|
| Authentication     | HTTP Basic                     | Wire existing JJWT dependency as Bearer tokens  |
| Database           | H2 in-memory                   | PostgreSQL / MySQL with Flyway migrations       |
| Recipe History     | Stub returning all recipes     | Event-sourced audit log or versioning table     |
| Search             | In-memory `findAll()` + filter | JPA Specification / Criteria API queries        |
| Export             | Raw entity JSON                | Versioned export DTO with schema documentation  |
| Frontend Auth      | In-memory credentials          | Secure cookie / token storage + refresh flow   |
