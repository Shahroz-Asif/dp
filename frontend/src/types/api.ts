// TypeScript interfaces that mirror the backend's DTOs and entity shapes exactly.

/** Mirrors ComponentResponse DTO returned inside RecipeResponse */
export interface ComponentResponse {
  id: number;
  name: string;
  description: string;
  modifiable: boolean;
  incompatibleConditionNames: string[];
}

/** Mirrors RecipeResponse DTO */
export interface RecipeResponse {
  id: number;
  name: string;
  description: string;
  mainComponent: ComponentResponse;
  modifiableComponents: ComponentResponse[];
}

/** Request body for POST /api/recipes and PUT /api/recipes/:id */
export interface RecipeRequest {
  name: string;
  description: string;
  mainComponentId: number;
  modifiableComponentIds: number[];
}

/** Mirrors PatientCondition entity */
export interface PatientCondition {
  id: number;
  name: string;
  description: string;
}

/** Mirrors RecipeComponent entity (polymorphic JSON with "type" discriminator) */
export interface RecipeComponent {
  id: number;
  /** Jackson discriminator: "MODIFIABLE" | "NON_MODIFIABLE" */
  type: 'MODIFIABLE' | 'NON_MODIFIABLE';
  name: string;
  description: string;
  modifiable: boolean;
  incompatibleConditions: PatientCondition[];
}
