import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useRecipes } from '../hooks/useRecipes';
import { buildAndStrategy } from '../patterns/searchStrategy';
import { usePatientProfile } from '../context/PatientProfileContext';
import { useAuth } from '../context/AuthContext';
import { orderRepository } from '../api/orders';
import type { MealCourse, MealType, RecipeResponse, ComponentResponse } from '../types/api';
import axios from 'axios';

const COURSES: Array<'ALL' | MealCourse> = ['ALL', 'BREAKFAST', 'LUNCH', 'DINNER'];
const MEAL_TYPES: Array<'ALL' | MealType> = ['ALL', 'MAIN', 'SIDE'];

const COURSE_ICONS: Record<string, string> = {
  BREAKFAST: '🌅',
  LUNCH: '☀️',
  DINNER: '🌙',
};

interface ComponentSelection {
  recipe: RecipeResponse;
  selectedIds: Set<number>;
}

export function RecipeListPage() {
  const { recipes, loading, error } = useRecipes();
  const { profileConditions } = usePatientProfile();
  const { role } = useAuth();
  const [courseFilter, setCourseFilter] = useState<'ALL' | MealCourse>('ALL');
  const [mealTypeFilter, setMealTypeFilter] = useState<'ALL' | MealType>('ALL');
  const [nameFilter, setNameFilter] = useState('');
  const [componentFilter, setComponentFilter] = useState('');
  const [profileFilterOn, setProfileFilterOn] = useState(false);
  const [orderingId, setOrderingId] = useState<number | null>(null);
  const [orderMsg, setOrderMsg] = useState<{ id: number; type: 'ok' | 'err'; text: string } | null>(null);
  const [componentSelection, setComponentSelection] = useState<ComponentSelection | null>(null);

  const strategy = buildAndStrategy({ name: nameFilter, componentName: componentFilter, mealType: mealTypeFilter === 'ALL' ? undefined : mealTypeFilter });
  const profileConditionNames = new Set(profileConditions.map((c) => c.name));

  const isProfileCompatible = (recipe: RecipeResponse) => {
    const allComponents = [recipe.mainComponent, ...recipe.modifiableComponents];
    return allComponents.every((comp) =>
      comp.incompatibleConditionNames.every((condName) => !profileConditionNames.has(condName))
    );
  };

  const isComponentCompatible = (comp: ComponentResponse) =>
    comp.incompatibleConditionNames.every((n) => !profileConditionNames.has(n));

  let filtered = recipes
    .filter((r) => courseFilter === 'ALL' || r.mealCourse === courseFilter)
    .filter((r) => strategy.matches(r));

  if (profileFilterOn && profileConditions.length > 0) {
    filtered = filtered.filter(isProfileCompatible);
  }

  const handleOrderClick = (recipe: RecipeResponse) => {
    if (recipe.modifiableComponents.length > 0) {
      setComponentSelection({ recipe, selectedIds: new Set() });
    } else {
      void submitOrder(recipe.id, []);
    }
  };

  const submitOrder = async (recipeId: number, selectedComponentIds: number[]) => {
    setOrderingId(recipeId);
    setOrderMsg(null);
    try {
      await orderRepository.placeOrder({ recipeId, selectedComponentIds });
      setOrderMsg({ id: recipeId, type: 'ok', text: 'Order placed!' });
      setComponentSelection(null);
    } catch (err) {
      const msg =
        axios.isAxiosError(err) && err.response?.data?.message
          ? err.response.data.message
          : 'Could not place order.';
      setOrderMsg({ id: recipeId, type: 'err', text: msg });
    } finally {
      setOrderingId(null);
    }
  };

  const toggleComponent = (id: number) => {
    if (!componentSelection) return;
    const next = new Set(componentSelection.selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setComponentSelection({ ...componentSelection, selectedIds: next });
  };

  if (loading) return <p className="page-loading">Loading recipes…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>🍽️ Menu</h2>
        {(role === 'DIETICIAN' || role === 'ADMIN') && (
          <Link to="/recipes/new" className="btn btn-primary">
            + New Recipe
          </Link>
        )}
      </div>

      {/* Meal course tabs */}
      <div className="course-tabs">
        {COURSES.map((course) => (
          <button
            key={course}
            className={`course-tab${courseFilter === course ? ' active' : ''}`}
            onClick={() => setCourseFilter(course)}
          >
            {course !== 'ALL' && <span style={{ marginRight: '0.35rem' }}>{COURSE_ICONS[course]}</span>}
            {course === 'ALL' ? 'All Meals' : course.charAt(0) + course.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Meal type tabs */}
      <div className="course-tabs" style={{ marginTop: '0.4rem' }}>
        {MEAL_TYPES.map((type) => (
          <button
            key={type}
            className={`course-tab${mealTypeFilter === type ? ' active' : ''}`}
            onClick={() => setMealTypeFilter(type)}
          >
            {type === 'ALL' ? 'All Types' : type.charAt(0) + type.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Search bar */}
      <div className="search-bar">
        <input
          type="search"
          placeholder="🔍  Search by recipe name…"
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
        />
        <input
          type="search"
          placeholder="🔍  Filter by component name…"
          value={componentFilter}
          onChange={(e) => setComponentFilter(e.target.value)}
        />
      </div>

      {/* Profile filter toggle */}
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
            const thisOrderMsg = orderMsg?.id === recipe.id ? orderMsg : null;
            const courseIcon = recipe.mealCourse ? COURSE_ICONS[recipe.mealCourse] : '🍽️';
            const isSelectingThisRecipe = componentSelection?.recipe.id === recipe.id;

            return (
              <div key={recipe.id} className="recipe-card recipe-card-block">
                {/* Card top: icon + title + badge */}
                <div className="recipe-card-title-row">
                  <Link to={`/recipes/${recipe.id}`} className="recipe-card-link">
                    <h3>{courseIcon} {recipe.name}</h3>
                  </Link>
                  {profileConditions.length > 0 && (
                    <span className={`badge ${compatible ? 'badge-ok' : 'badge-warn'}`}>
                      {compatible ? '✓ Safe' : '✗ Conflicts'}
                    </span>
                  )}
                </div>

                <p>{recipe.description || 'No description.'}</p>

                <div className="recipe-card-badges">
                  <span className={`meal-course-badge course-${recipe.mealCourse?.toLowerCase()}`}>
                    {recipe.mealCourse}
                  </span>
                  <span className={`meal-type-badge type-${recipe.mealType?.toLowerCase()}`}>
                    {recipe.mealType}
                  </span>
                </div>

                <div className="recipe-card-meta">
                  <span>🥘 {recipe.mainComponent.name}</span>
                  <span>{recipe.modifiableComponents.length} optional</span>
                </div>

                {role === 'PATIENT' && (
                  <div className="recipe-card-order">
                    {thisOrderMsg && (
                      <p className={thisOrderMsg.type === 'ok' ? 'order-success' : 'order-error'}>
                        {thisOrderMsg.text}
                      </p>
                    )}

                    {/* Inline component selector */}
                    {isSelectingThisRecipe && componentSelection && (
                      <div className="component-selector">
                        <p className="component-selector-title">Choose optional components:</p>
                        {componentSelection.recipe.modifiableComponents.map((comp) => {
                          const compCompatible = isComponentCompatible(comp);
                          return (
                            <label
                              key={comp.id}
                              className={`component-selector-item${!compCompatible ? ' comp-incompatible' : ''}`}
                            >
                              <input
                                type="checkbox"
                                disabled={!compCompatible}
                                checked={componentSelection.selectedIds.has(comp.id)}
                                onChange={() => toggleComponent(comp.id)}
                              />
                              <span>{comp.name}</span>
                              {!compCompatible && (
                                <span className="comp-conflict-hint">
                                  (conflicts: {comp.incompatibleConditionNames.filter((n) => profileConditionNames.has(n)).join(', ')})
                                </span>
                              )}
                            </label>
                          );
                        })}
                        <div className="component-selector-actions">
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={!!orderingId}
                            onClick={() =>
                              submitOrder(recipe.id, Array.from(componentSelection.selectedIds))
                            }
                          >
                            {orderingId === recipe.id ? 'Ordering…' : 'Confirm Order'}
                          </button>
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setComponentSelection(null)}
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    )}

                    {!isSelectingThisRecipe && (
                      <>
                        {!compatible && profileConditions.length > 0 && (
                          <p className="order-error">Cannot order — conflicts with your profile.</p>
                        )}
                        <button
                          className="btn btn-primary btn-sm"
                          disabled={!!orderingId || (!compatible && profileConditions.length > 0)}
                          title={
                            !compatible && profileConditions.length > 0
                              ? 'This recipe contains ingredients that conflict with your medical profile'
                              : undefined
                          }
                          onClick={() => handleOrderClick(recipe)}
                        >
                          {orderingId === recipe.id ? 'Ordering…' : 'Order Now'}
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

