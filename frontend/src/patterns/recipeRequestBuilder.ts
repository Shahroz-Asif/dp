import type { RecipeRequest } from '../types/api';

/**
 * Builder Pattern — constructs a validated RecipeRequest step-by-step.
 *
 * Mirrors the backend's RecipeBuilder: enforces required fields and throws
 * descriptive errors on build() if invariants are violated. The form page
 * uses this so validation is in one place, not scattered across event handlers.
 */
export class RecipeRequestBuilder {
  private _name = '';
  private _description = '';
  private _mainComponentId: number | null = null;
  private _modifiableComponentIds: number[] = [];

  name(value: string): this {
    this._name = value;
    return this;
  }

  description(value: string): this {
    this._description = value;
    return this;
  }

  mainComponent(id: number): this {
    this._mainComponentId = id;
    return this;
  }

  modifiableComponents(ids: number[]): this {
    this._modifiableComponentIds = [...ids];
    return this;
  }

  /** Validates all required fields and returns the complete request object */
  build(): RecipeRequest {
    if (!this._name.trim()) {
      throw new Error('Recipe name is required.');
    }
    if (this._mainComponentId === null) {
      throw new Error('A main (non-modifiable) component must be selected.');
    }
    return {
      name: this._name.trim(),
      description: this._description.trim(),
      mainComponentId: this._mainComponentId,
      modifiableComponentIds: this._modifiableComponentIds,
    };
  }
}
