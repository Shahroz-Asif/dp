# RecipeMaker — Patient Recipe System

A Java Spring Boot backend for a patient recipe system using design patterns.

## Design Patterns

### Builder Pattern (`RecipeBuilder`)
Recipes are constructed step-by-step using `RecipeBuilder`. The builder enforces that:
- A **main component** (non-modifiable) must be provided
- Only modifiable components can be added as optional components
- A name is required

### Composite Strategy Pattern (Search/Filter)
Recipe searching uses composable strategy objects:
- `RecipeSearchStrategy` — interface for individual filter strategies
- `NameSearchStrategy` — filter by recipe name keyword
- `ComponentNameSearchStrategy` — filter by component name keyword
- `ConditionCompatibilityStrategy` — filter recipes whose main component is compatible with given conditions
- `CompositeAndSearchStrategy` — composes strategies with AND logic
- `CompositeOrSearchStrategy` — composes strategies with OR logic

### Repository Pattern
Spring Data JPA repositories abstract data access for all entities.

## Compatibility Rules

- **Main component incompatible** → entire recipe is **unselectable**
- **Modifiable component incompatible** → only that component is **unselectable**; the recipe and other compatible components remain selectable

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |

### Recipes
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/recipes` | List all recipes |
| GET | `/api/recipes/{id}` | Get recipe by ID |
| POST | `/api/recipes` | Create recipe (Builder pattern) |
| PUT | `/api/recipes/{id}` | Update recipe |
| DELETE | `/api/recipes/{id}` | Delete recipe |
| POST | `/api/recipes/{id}/check-compatibility` | Check compatibility with conditions |
| GET | `/api/recipes/search` | Search/filter recipes (Composite Strategy) |

### Components
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/components` | List all components |
| GET | `/api/components/{id}` | Get component by ID |
| POST | `/api/components` | Create component |
| PUT | `/api/components/{id}` | Update component |
| DELETE | `/api/components/{id}` | Delete component |

### Patient Conditions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/conditions` | List all conditions |
| GET | `/api/conditions/{id}` | Get condition by ID |
| POST | `/api/conditions` | Create condition |
| PUT | `/api/conditions/{id}` | Update condition |
| DELETE | `/api/conditions/{id}` | Delete condition |

### Patient Profiles
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/patients` | List all patient profiles |
| GET | `/api/patients/{id}` | Get profile by ID |
| POST | `/api/patients` | Create profile |
| PUT | `/api/patients/{id}` | Update profile |
| DELETE | `/api/patients/{id}` | Delete profile |

### Recipe Export/Import
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/recipes/export` | Export all recipes |
| POST | `/api/recipes/export` | Import recipes |

### Recipe History
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/recipes/history/{id}` | Get recipe history |

## Swagger UI

API documentation available at: `http://localhost:8080/swagger-ui.html`

## Getting Started

### Prerequisites
- Java 17+
- Maven

### Build & Run
```
mvn clean compile
mvn spring-boot:run
```

### H2 Console
Available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:recipemaker`)
