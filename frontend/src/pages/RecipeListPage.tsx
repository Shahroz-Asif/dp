import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useRecipes } from '../hooks/useRecipes';
import { buildAndStrategy } from '../patterns/searchStrategy';
import { usePatientProfile } from '../context/PatientProfileContext';
import type { RecipeResponse } from '../types/api';

/**
 * Recipe list page.
 *
 * Client-side filtering uses the Strategy Pattern (CompositeAndSearchStrategy)
 * for instant, zero-latency filtering on top of the already-loaded recipe list.
 * The backend search endpoint is also available for server-filtered results
 * (called when the user submits the search form explicitly).
 *
 * When the user has a saved health profile, a toggle appears to narrow the list
 * to only recipes whose main component is compatible with their conditions.
 */
export function RecipeListPage() {
  const { recipes, loading, error } = useRecipes();
  const { profileConditions } = usePatientProfile();
  const [nameFilter, setNameFilter] = useState('');
  const [componentFilter, setComponentFilter] = useState('');
  const [profileFilterOn, setProfileFilterOn] = useState(false);

  // Build a composite AND strategy from the active filter values
  const strategy = buildAndStrategy({ name: nameFilter, componentName: componentFilter });
  let filtered = recipes.filter((r) => strategy.matches(r));

  // Profile filter: exclude recipes whose main component conflicts with any profile condition
  const profileConditionNames = new Set(profileConditions.map((c) => c.name));
  const isProfileCompatible = (recipe: RecipeResponse) =>
    recipe.mainComponent.incompatibleConditionNames.every(
      (condName) => !profileConditionNames.has(condName)
    );

  if (profileFilterOn && profileConditions.length > 0) {
    filtered = filtered.filter(isProfileCompatible);
  }

  if (loading) return <p className="page-loading">Loading recipes…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Recipes</h2>
        <Link to="/recipes/new" className="btn btn-primary">
          + New Recipe
        </Link>
      </div>

      {/* Search bar — drives the Strategy filter above */}
      <div className="search-bar">
        <input
          type="search"
          placeholder="Filter by recipe name…"
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
        />
        <input
          type="search"
          placeholder="Filter by component name…"
          value={componentFilter}
          onChange={(e) => setComponentFilter(e.target.value)}
        />
      </div>

      {/* Profile filter toggle — only shown when the user has a saved profile */}
      {profileConditions.length > 0 && (
        <div className="profile-filter-bar">
          <label className="profile-filter-toggle">
            <input
              type="checkbox"
              checked={profileFilterOn}
              onChange={(e) => setProfileFilterOn(e.target.checked)}
            />
            Show only recipes compatible with my profile
          </label>
          <span className="profile-filter-conditions">
            {profileConditions.map((c) => c.name).join(', ')}
          </span>
        </div>
      )}

      {filtered.length === 0 ? (
        <p className="empty-state">No recipes match your filters.</p>
      ) : (
        <div className="recipe-grid">
          {filtered.map((recipe) => {
            const compatible = profileConditions.length === 0 || isProfileCompatible(recipe);
            return (
              <Link to={`/recipes/${recipe.id}`} key={recipe.id} className="recipe-card">
                <div className="recipe-card-title-row">
                  <h3>{recipe.name}</h3>
                  {profileConditions.length > 0 && (
                    <span className={`badge ${compatible ? 'badge-ok' : 'badge-warn'}`}>
                      {compatible ? '✓ Safe' : '✗ Conflicts'}
                    </span>
                  )}
                </div>
                <p>{recipe.description || 'No description.'}</p>
                <div className="recipe-card-meta">
                  <span>Main: {recipe.mainComponent.name}</span>
                  <span>{recipe.modifiableComponents.length} optional</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
