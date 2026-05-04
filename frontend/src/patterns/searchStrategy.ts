import type { MealType, RecipeResponse } from '../types/api';

/**
 * Strategy Pattern (mirroring the backend's search package).
 *
 * RecipeSearchStrategy is the common interface. Concrete strategies
 * encapsulate a single filter rule. Composite strategies combine them
 * with AND / OR semantics — exactly as the backend does.
 *
 * Usage: build a CompositeAndSearchStrategy from whatever the user typed,
 * then call strategy.matches(recipe) on each item in the local recipe list
 * for instant, no-round-trip filtering.
 */
export interface RecipeSearchStrategy {
  matches(recipe: RecipeResponse): boolean;
}

// ─── Leaf strategies ────────────────────────────────────────────────────────

/** Filters recipes whose name contains the keyword (case-insensitive) */
export class NameSearchStrategy implements RecipeSearchStrategy {
  constructor(private readonly keyword: string) {}

  matches(recipe: RecipeResponse): boolean {
    return recipe.name.toLowerCase().includes(this.keyword.toLowerCase());
  }
}

/** Filters recipes where any component's name contains the keyword */
export class ComponentNameSearchStrategy implements RecipeSearchStrategy {
  constructor(private readonly keyword: string) {}

  matches(recipe: RecipeResponse): boolean {
    const all = [recipe.mainComponent, ...recipe.modifiableComponents];
    return all.some((c) => c.name.toLowerCase().includes(this.keyword.toLowerCase()));
  }
}

/** Filters recipes by meal type (MAIN or SIDE) */
export class MealTypeSearchStrategy implements RecipeSearchStrategy {
  constructor(private readonly mealType: MealType) {}

  matches(recipe: RecipeResponse): boolean {
    return recipe.mealType === this.mealType;
  }
}

// ─── Composite strategies ────────────────────────────────────────────────────

/**
 * Composite AND — all child strategies must match.
 * An empty composite matches everything (identity for AND).
 */
export class CompositeAndSearchStrategy implements RecipeSearchStrategy {
  constructor(private readonly strategies: RecipeSearchStrategy[]) {}

  matches(recipe: RecipeResponse): boolean {
    return this.strategies.every((s) => s.matches(recipe));
  }
}

/**
 * Composite OR — at least one child strategy must match.
 * An empty composite matches everything (identity for OR).
 */
export class CompositeOrSearchStrategy implements RecipeSearchStrategy {
  constructor(private readonly strategies: RecipeSearchStrategy[]) {}

  matches(recipe: RecipeResponse): boolean {
    if (this.strategies.length === 0) return true;
    return this.strategies.some((s) => s.matches(recipe));
  }
}

// ─── Factory helper ──────────────────────────────────────────────────────────

/** Build a CompositeAndSearchStrategy from the search form's current values */
export function buildAndStrategy(filters: {
  name?: string;
  componentName?: string;
  mealType?: MealType;
}): CompositeAndSearchStrategy {
  const strategies: RecipeSearchStrategy[] = [];
  if (filters.name?.trim()) {
    strategies.push(new NameSearchStrategy(filters.name.trim()));
  }
  if (filters.componentName?.trim()) {
    strategies.push(new ComponentNameSearchStrategy(filters.componentName.trim()));
  }
  if (filters.mealType) {
    strategies.push(new MealTypeSearchStrategy(filters.mealType));
  }
  return new CompositeAndSearchStrategy(strategies);
}
