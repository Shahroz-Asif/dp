// TypeScript interfaces that mirror the backend's DTOs and entity shapes exactly.

export type MealCourse = 'BREAKFAST' | 'LUNCH' | 'DINNER';
export type MealType = 'MAIN' | 'SIDE';
export type OrderStatus = 'REQUESTED' | 'PREPARING' | 'READY' | 'DONE';
export type UserRole = 'ADMIN' | 'DOCTOR' | 'DIETICIAN' | 'PATIENT' | 'KITCHEN';

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
  mealCourse: MealCourse;
  mealType: MealType;
  createdByUsername?: string;
}

/** Request body for POST /api/recipes and PUT /api/recipes/:id */
export interface RecipeRequest {
  name: string;
  description: string;
  mainComponentId: number;
  modifiableComponentIds: number[];
  mealCourse: MealCourse;
  mealType: MealType;
}

/** Mirrors PatientConditionResponse DTO */
export interface PatientCondition {
  id: number;
  name: string;
  description: string;
  createdByDoctorName?: string;
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

/** Mirrors UserResponse DTO from GET /api/auth/me */
export interface UserResponse {
  id: number;
  username: string;
  role: UserRole;
  patientProfileId?: number;
}

/** Request body for POST /api/orders */
export interface MealOrderRequest {
  recipeId: number;
  /** IDs of the modifiable components the patient selected (empty = none) */
  selectedComponentIds: number[];
}

/** Mirrors MealOrderResponse DTO */
export interface MealOrderResponse {
  id: number;
  patientProfileId: number;
  patientName: string;
  recipeId: number;
  recipeName: string;
  mealCourse: MealCourse;
  mealType: MealType;
  status: OrderStatus;
  orderDate: string;
  createdAt: string;
  selectedComponents: ComponentResponse[];
}

/** Request body for patient profile create/update */
export interface PatientProfileRequest {
  name: string;
  age: number;
  notes?: string;
}

/** Mirrors PatientProfileResponse DTO */
export interface PatientProfileResponse {
  id: number;
  name: string;
  age: number;
  notes?: string;
  assignedDoctorUsername?: string;
  conditions: PatientCondition[];
}
