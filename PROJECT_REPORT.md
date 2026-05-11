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
| Database       | PostgreSQL 16 (runtime); H2 (tests only) |
| Auth           | HTTP Basic (bcrypt-hashed)         |
| Realtime       | STOMP over WebSocket (SockJS)      |
| API Style      | RESTful JSON                       |
| API Docs       | OpenAPI 3 / Swagger UI             |
| Deployment     | Docker Compose (app + db)          |

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
| **PostgreSQL 16**                | Persistent relational database (runtime)          |
| **H2 Database**                  | In-memory database for tests only                 |
| **Spring WebSocket + STOMP**     | Real-time push notifications over WebSocket       |
| **Lombok**                       | Boilerplate reduction (`@Data`, `@Builder`, etc.) |
| **JJWT 0.11.5**                  | JWT library (provisioned for token upgrade path)  |
| **SpringDoc OpenAPI 2.5.0**      | Auto-generated Swagger UI at `/swagger-ui.html`   |
| **Maven**                        | Build tool and dependency management              |

### Frontend

| Technology              | Role                                                      |
|-------------------------|-----------------------------------------------------------|
| **React 18**            | Component-based UI library with concurrent rendering      |
| **TypeScript 5**        | Static typing across the entire frontend codebase         |
| **Vite 5.2**            | Build tool and dev server (HMR, fast cold starts)         |
| **React Router v6**     | Client-side routing with nested route layouts             |
| **Axios 1.x**           | HTTP client for REST API communication                    |
| **@stomp/stompjs v7**   | STOMP protocol client for WebSocket messaging             |
| **sockjs-client 1.6**   | WebSocket transport with automatic fallback               |
| **Custom CSS**          | Hand-crafted responsive stylesheet (`index.css`)          |

---

## System Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Browser / Client                 │
│                                                     │
│   React 18 + TypeScript   (Vite dev server :5173)   │
│         STOMP/SockJS WebSocket client                │
│   ┌──────────┐  ┌────────────┐  ┌───────────────┐  │
│   │  Pages   │  │ Components │  │  API Layer    │  │
│   │ (Router) │  │ (shadcn/ui)│  │  (Axios +     │  │
│   └──────────┘  └────────────┘  │  TanStack Q.) │  │
│                                 └───────┬───────┘  │
└─────────────────────────────────────────┼───────────┘
                          HTTP Basic Auth │ JSON REST
┌─────────────────────────────────────────┼───────────┐
│                Spring Boot :8081        │           │
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
│   │  PostgreSQL 16 Database  │                      │
│   └──────────────────────────┘                      │
│                                                     │
│ (Tests use H2 in-memory — no external DB required)  │
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
| `Recipe`                | `id`, `name`, `description`, `course`, `mealType`, `imageUrl`, `mainComponent`, `modifiableComponents` | Built via `RecipeBuilder` |

### API Endpoints

All endpoints require HTTP Basic authentication unless marked **Public**.

#### Authentication — `/api/auth`

| Method | Path                  | Auth   | Description                            |
|--------|-----------------------|--------|----------------------------------------|
| `POST` | `/api/auth/register`  | Public | Register a new user; body: `{username, password, role}` (DOCTOR/DIETICIAN/PATIENT/KITCHEN); returns `UserResponse` JSON; 409 on duplicate username |
| `GET`  | `/api/auth/me`        | Any    | Returns current authenticated user info |

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

#### Meal Orders — `/api/orders`

| Method | Path                   | Auth     | Description                                          |
|--------|------------------------|----------|------------------------------------------------------|
| `POST` | `/api/orders`          | PATIENT  | Place a meal order with selected component IDs       |
| `GET`  | `/api/orders/active`   | PATIENT  | Get active orders for the current patient            |
| `GET`  | `/api/orders/history`  | PATIENT  | Get completed orders for the current patient         |

#### Kitchen — `/api/kitchen`

| Method | Path                          | Auth            | Description                              |
|--------|-------------------------------|-----------------|------------------------------------------|
| `GET`  | `/api/kitchen/orders`         | KITCHEN / ADMIN | List all active orders across all patients |
| `PUT`  | `/api/kitchen/orders/{id}/advance` | KITCHEN / ADMIN | Advance order status: `REQUESTED → PREPARING → READY → DONE` |

#### Doctor — `/api/doctor`

| Method   | Path                                               | Auth           | Description                           |
|----------|----------------------------------------------------|----------------|---------------------------------------|
| `GET`    | `/api/doctor/patients`                             | DOCTOR / ADMIN | List patients visible to this doctor  |
| `POST`   | `/api/doctor/patients/{patientId}/conditions/{conditionId}` | DOCTOR / ADMIN | Assign a condition to a patient |
| `DELETE` | `/api/doctor/patients/{patientId}/conditions/{conditionId}` | DOCTOR / ADMIN | Remove a condition from a patient |

#### Notifications — `/api/notifications`

| Method | Path                         | Auth | Description                                  |
|--------|------------------------------|------|----------------------------------------------|
| `GET`  | `/api/notifications`         | Any  | Get all notifications for the current user   |
| `PUT`  | `/api/notifications/{id}/read` | Any | Mark a single notification as read          |
| `PUT`  | `/api/notifications/read-all`| Any  | Mark all notifications for the current user as read |

### Security

- **Mechanism**: HTTP Basic Authentication over every secured route
- **Password storage**: BCrypt via Spring Security's `BCryptPasswordEncoder`
- **Public routes**: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- **CSRF**: Disabled (stateless REST API)
- **Role-based access**: `@PreAuthorize` enforced per endpoint; PATIENT/DOCTOR/DIETICIAN/KITCHEN/ADMIN roles (ADMIN not registerable via UI)
- **Registration**: `POST /api/auth/register` is public; enforces role allow-list; returns 409 on duplicate username
- **JWT provisioned**: `jjwt-api/impl/jackson 0.11.5` are present on the classpath; a filter-chain upgrade to Bearer token auth is the natural next step
- **User storage**: `AppUser` entity in PostgreSQL (H2 for tests), loaded via `UserDetailsServiceImpl`

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

#### Observer Pattern — `OrderEventObserver`

Order lifecycle events are broadcast to registered observers without coupling the `MealOrderService` to downstream logic:

```java
public interface OrderEventObserver {
    void onOrderPlaced(MealOrder order);
    void onOrderStatusAdvanced(MealOrder order, OrderStatus fromStatus);
}
```

`NotificationService` implements `OrderEventObserver` and creates persistent `Notification` records (and pushes them via WebSocket) when an order is placed or its status changes. Adding a new observer (e.g., email dispatch) requires only implementing the interface and registering it — `MealOrderService` needs no modification.

#### Repository Pattern

Six `JpaRepository` interfaces (`RecipeRepository`, `RecipeComponentRepository`, `PatientConditionRepository`, `PatientProfileRepository`, `AppUserRepository`, `MealOrderRepository`) fully abstract all data access, leaving the service layer decoupled from persistence details.

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

On first startup, 5 patient conditions, 15 recipe components, **15 recipes** (each with an Unsplash image URL), 6 pre-created users, and 2 patient profiles are seeded. The seeder is idempotent — it guards on `userRepo.count() > 0` so it never re-seeds after the first run, making it safe with persistent PostgreSQL storage.

**Main-component compatibility check** (`MealOrderService.placeOrder`):

Before processing any optional components the service validates that the recipe's non-modifiable main component is not contraindicated by any of the patient's conditions. If it is, a `400 Bad Request` is returned immediately, regardless of optional component selection.

---

## Frontend

### Frontend Technology Stack

The frontend is a single-page application built with **React 18** and **TypeScript 5**, bundled by **Vite** for near-instant hot-module replacement during development.

| Layer              | Library / Tool         | Version  | Purpose                                          |
|--------------------|------------------------|----------|-------------------------------------------------|
| UI Framework       | React                  | 18.3     | Component tree, concurrent rendering             |
| Language           | TypeScript             | 5.4      | End-to-end static typing                         |
| Build Tool         | Vite                   | 5.2      | Dev server (HMR), production bundling (Rollup)   |
| Routing            | React Router           | 6.x      | Declarative nested routes, `<Outlet>` layouts    |
| HTTP Client        | Axios                  | 1.7      | Interceptors for Basic Auth header injection     |
| WebSocket          | @stomp/stompjs         | 7.3      | STOMP protocol over WebSocket for push notifications |
| WS Transport       | sockjs-client          | 1.6      | WebSocket with automatic long-polling fallback   |
| Styling            | Custom CSS             | —        | Hand-crafted responsive stylesheet (`index.css`) |

### Application Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── client.ts              # Axios instance; injects Basic Auth header
│   │   ├── recipes.ts             # Recipe CRUD + search + compatibility calls
│   │   ├── components.ts          # Component CRUD calls
│   │   ├── conditions.ts          # Condition CRUD calls
│   │   ├── patients.ts            # Patient profile CRUD calls
│   │   ├── orders.ts              # Meal order placement + history calls
│   │   ├── kitchen.ts             # Kitchen order management calls
│   │   ├── doctor.ts              # Doctor patient/condition management calls
│   │   └── notifications.ts       # Notification fetch + mark-read calls
│   ├── hooks/
│   │   ├── useRecipes.ts          # Recipe data hooks
│   │   ├── useComponents.ts       # Component data hooks
│   │   └── useConditions.ts       # Condition data hooks
│   ├── pages/
│   │   ├── LoginPage.tsx          # HTTP Basic credential form + "Create Account" modal
│   │   ├── RecipeListPage.tsx     # Recipe browser: images, search, PlaceOrderModal
│   │   ├── RecipeDetailPage.tsx   # Recipe detail + compatibility checker
│   │   ├── RecipeFormPage.tsx     # Create / edit recipe form
│   │   ├── ComponentsPage.tsx     # Component management table
│   │   ├── ConditionsPage.tsx     # Condition management table
│   │   ├── PatientProfilePage.tsx # Patient's own profile view
│   │   ├── ActiveOrdersPage.tsx   # Patient's in-flight orders
│   │   ├── OrderHistoryPage.tsx   # Patient's completed order history
│   │   ├── KitchenPage.tsx        # Kitchen queue + advance-status controls
│   │   └── DoctorPage.tsx         # Doctor's patient list + condition assignment
│   ├── components/
│   │   ├── layout/
│   │   │   └── AppShell.tsx       # Sidebar nav + topbar wrapper
│   │   ├── PrivateRoute.tsx        # Redirects to /login if unauthenticated
│   │   ├── PlaceOrderModal.tsx     # Component-selection modal for ordering
│   │   ├── RegisterModal.tsx       # Self-registration dialog (from login page)
│   │   ├── NotificationBell.tsx    # Bell icon with unread count + dropdown
│   │   └── StatusBadge.tsx         # Selectable / Incompatible status badges
│   ├── context/
│   │   ├── AuthContext.tsx         # Stores credentials; provides useAuth()
│   │   ├── PatientProfileContext.tsx # Lazy-loads patient profile for PATIENT role
│   │   └── NotificationContext.tsx  # STOMP/SockJS WebSocket connection + message queue
│   ├── patterns/
│   │   ├── searchStrategy.ts       # Frontend Strategy pattern mirroring backend search
│   │   └── recipeRequestBuilder.ts # Builder pattern for recipe request DTOs
│   ├── types/
│   │   └── api.ts                  # TypeScript interfaces mirroring backend DTOs
│   ├── App.tsx                     # Route definitions
│   ├── main.tsx                    # React DOM entry point
│   └── index.css                   # Global responsive stylesheet
├── index.html
├── vite.config.ts                  # Proxy /api + /ws → http://localhost:8081
└── tsconfig.json
```

### Pages & Features

#### Login Page (`/login`)

- Accepts `username` and `password` via a plain controlled form
- On submit, stores credentials in `AuthContext` (in-memory; no localStorage persistence of raw passwords)
- Performs a validation `GET /api/auth/me` call before redirecting
- Protected pages redirect here via `PrivateRoute` when credentials are absent
- **"Create Account" button** opens `RegisterModal`, allowing new users to self-register; on success the new credentials are auto-submitted to log in immediately

#### Recipe List Page (`/recipes`)

- Fetches all recipes via `useRecipes()` (Axios + React state)
- Each recipe card shows a **full-width image banner** (from the `imageUrl` field, served via Unsplash CDN)
- **Search bar** with name keyword, component name keyword, and meal-type/course tabs; filtering is performed client-side via the `CompositeAndSearchStrategy`
- **"Order Now"** button (PATIENT role): opens `PlaceOrderModal` showing optional components as checkboxes; components incompatible with the patient's conditions are disabled with a warning tag; main-component incompatibility shows a card-level warning and disables the button
- **Compatibility badge**: red "Conflicts" ribbon when the recipe's main component conflicts with the logged-in patient's conditions
- "New Recipe" / "Edit" / "Delete" action buttons visible to DIETICIAN/ADMIN roles

#### Recipe Detail Page (`/recipes/:id`)

- Fetches recipe with `GET /api/recipes/{id}`
- Displays main component (highlighted) and all modifiable components with type badges
- **Compatibility Panel**: multi-select of available conditions; calls `POST /api/recipes/{id}/check-compatibility`
  - Overall recipe selectability shown with a colored banner
  - Per-component breakdown with incompatible condition names
- Edit / Delete action buttons (DIETICIAN / ADMIN roles)

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

#### Patient Profile Page (`/profile`)

- Displays the current PATIENT user's own profile (name, age, assigned conditions)
- Editable fields for notes and personal information

#### Active Orders Page (`/orders/active`)

- Lists all in-flight orders for the current PATIENT
- Each order shows recipe name, status badge (`REQUESTED` / `PREPARING` / `READY`), and the list of selected optional components

#### Order History Page (`/orders/history`)

- Lists all `DONE` orders for the current PATIENT with the same component detail view

#### Kitchen Page (`/kitchen`)

- Accessible to KITCHEN role only
- Lists all active orders across all patients
- **Advance Status** button moves each order along its lifecycle: `REQUESTED → PREPARING → READY → DONE`; each transition triggers a real-time WebSocket push to the relevant patient

#### Doctor Page (`/doctor`)

- Accessible to DOCTOR role only
- Lists patients assigned to the current doctor
- Inline controls to assign or remove `PatientCondition` entries from each patient's profile

### API Integration

**`api/client.ts`** creates a pre-configured Axios instance:

```typescript
import axios from 'axios';

const apiClient = axios.create({ baseURL: '/api' });

apiClient.interceptors.request.use((config) => {
  const creds = sessionStorage.getItem('credentials');
  if (creds) {
    config.headers['Authorization'] = 'Basic ' + creds;
  }
  return config;
});

export default apiClient;
```

Vite's dev server proxies `/api/*` → `http://localhost:8081/api/*` and `/ws` → `ws://localhost:8081/ws`, eliminating CORS issues during development.

**WebSocket notifications** (`NotificationContext.tsx`) use STOMP over SockJS:

```typescript
const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { login: username, passcode: password },
});
client.onConnect = () => {
  client.subscribe('/user/queue/notifications', (msg) => {
    setNotifications(prev => [JSON.parse(msg.body), ...prev]);
  });
};
```

**TypeScript DTO mirrors** (`types/api.ts`) align exactly with the backend response shapes:

```typescript
export interface RecipeResponse {
  id: number;
  name: string;
  description: string;
  course: string;            // BREAKFAST | LUNCH | DINNER
  mealType: string;          // MAIN | SIDE
  imageUrl?: string;         // Unsplash CDN URL
  mainComponent: ComponentResponse;
  modifiableComponents: ComponentResponse[];
}

export interface MealOrderRequest {
  recipeId: number;
  selectedComponentIds: number[];
}

export interface MealOrderResponse {
  id: number;
  recipeId: number;
  recipeName: string;
  status: string;            // REQUESTED | PREPARING | READY | DONE
  selectedComponents: ComponentResponse[];
}
```

---

## Testing

### Backend Test Suite

All 120 tests run against **H2 in-memory** (`MODE=PostgreSQL`, `create-drop`) via `src/test/resources/application.properties`. No external database is required to run the suite.

| Test Class                        | Type                                    | Tests | Coverage area                                               |
|-----------------------------------|-----------------------------------------|-------|-------------------------------------------------------------|
| `RecipeBuilderTest`               | Unit (no Spring context)                | 9     | `RecipeBuilder` validation rules and fluent API             |
| `SearchStrategyTest`              | Unit (no Spring context)                | 17    | All 5 search strategy classes, AND/OR composite logic       |
| `MealOrderServiceTest`            | `@SpringBootTest`                       | 15    | Order placement, selected components, status advancement    |
| `NotificationServiceTest`         | `@SpringBootTest`                       | 13    | Observer pattern, mark-read, per-user filtering             |
| `RecipeServiceIntegrationTest`    | `@SpringBootTest @Transactional`        | 37    | Data seeder, CRUD, compatibility rules, search combinations |
| `ControllerIntegrationTest`       | `@SpringBootTest @AutoConfigureMockMvc` | 29    | All REST endpoints; auth enforcement; 401 on unauthenticated; registration |

### Frontend Test Strategy

Frontend testing is not yet configured. The TypeScript compilation (`tsc --noEmit`) acts as the primary static verification step.

---

## Developer Experience

### Option A — Docker Compose (recommended)

```bash
# Builds the full app image, starts PostgreSQL + app
docker compose up --build
# App: http://localhost:8081
# Swagger UI: http://localhost:8081/swagger-ui.html
```

Data is persisted in a Docker named volume `pgdata`. The `app` service will not start until PostgreSQL reports healthy via `pg_isready`.

### Option B — Local Development

```bash
# 1. Start a local PostgreSQL instance
docker run -d --name pg \
  -e POSTGRES_DB=recipemaker \
  -e POSTGRES_USER=recipemaker \
  -e POSTGRES_PASSWORD=recipemaker \
  -p 5432:5432 postgres:16-alpine

# 2. Start the backend (Java 21 + Maven 3.9+)
cd backend && ./mvnw spring-boot:run
# API: http://localhost:8081
# Swagger UI: http://localhost:8081/swagger-ui.html

# 3. Start the frontend (Node 20+)
cd frontend && npm install && npm run dev
# App: http://localhost:5173
```

> The Vite dev server proxies `/api` and `/ws` to `localhost:8081` — no CORS configuration required.

### Running Tests

```bash
cd backend && ./mvnw test
# 120 tests, no external database required (uses H2 in-memory)
```

### Default Seeded Accounts

On first startup `DataSeeder` creates 6 ready-to-use accounts:

| Username     | Password   | Role       |
|--------------|------------|------------|
| `doctor1`    | `password` | DOCTOR     |
| `dietician1` | `password` | DIETICIAN  |
| `patient1`   | `password` | PATIENT    |
| `patient2`   | `password` | PATIENT    |
| `kitchen1`   | `password` | KITCHEN    |
| `admin`      | `password` | ADMIN      |

New accounts can be created via the **"Create Account"** button on the login page (roles: DOCTOR / DIETICIAN / PATIENT / KITCHEN; ADMIN is not self-registerable).

### API Documentation

Live Swagger UI at `http://localhost:8081/swagger-ui.html` documents all endpoints with request/response schemas, powered by SpringDoc OpenAPI 2.5.0.

---

## Potential Upgrade Paths

| Area               | Current                             | Natural Next Step                              |
|--------------------|-------------------------------------|------------------------------------------------|
| Authentication     | HTTP Basic                          | Wire existing JJWT dependency as Bearer tokens |
| Database Migrations| `ddl-auto=update`                   | Flyway or Liquibase versioned migrations        |
| Recipe History     | Stub returning all recipes          | Event-sourced audit log or versioning table     |
| Search             | In-memory `findAll()` + filter      | JPA Specification / Criteria API queries        |
| Export             | Raw entity JSON                     | Versioned export DTO with schema documentation  |
| Frontend Auth      | In-memory credentials (sessionStorage) | HttpOnly cookie / refresh token flow          |
| Frontend State     | Plain `useState` + Axios            | TanStack Query for caching and background sync  |
