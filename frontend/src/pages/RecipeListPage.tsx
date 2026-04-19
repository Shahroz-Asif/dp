import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useRecipes } from '../hooks/useRecipes';
import { buildAndStrategy } from '../patterns/searchStrategy';
import { usePatientProfile } from '../context/PatientProfileContext';
import { useAuth } from '../context/AuthContext';
import { orderRepository } from '../api/orders';
import type { MealCourse, RecipeResponse } from '../types/api';
import axios from 'axios';

const COURSES: Array<'ALL' | MealCourse> = ['ALL', 'BREAKFAST', 'LUNCH', 'DINNER'];

const COURSE_ICONS: Record<string, string> = {
  BREAKFAST: '🌅',
  LUNCH: '☀️',
  DINNER: '🌙',
};

export function RecipeListPage() {
  const { recipes, loading, error } = useRecipes();
  const { profileConditions } = usePatientProfile();
  const { role } = useAuth();
  const [courseFilter, setCourseFilter] = useState<'ALL' | MealCourse>('ALL');
  const [nameFilter, setNameFilter] = useState('');
  const [componentFilter, setComponentFilter] = useState('');
  const [profileFilterOn, setProfileFilterOn] = useState(false);
  const [orderingId, setOrderingId] = useState<number | null>(null);
  const [orderMsg, setOrderMsg] = useState<{ id: number; type: 'ok' | 'err'; text: string } | null>(null);

  const strategy = buildAndStrategy({ name: nameFilter, componentName: componentFilter });
  const profileConditionNames = new Set(profileConditions.map((c) => c.name));

  const isProfileCompatible = (recipe: RecipeResponse) =>
    recipe.mainComponent.incompatibleConditionNames.every(
      (condName) => !profileConditionNames.has(condName)
    );

  let filtered = recipes
    .filter((r) => courseFilter === 'ALL' || r.mealCourse === courseFilter)
    .filter((r) => strategy.matches(r));

  if (profileFilterOn && profileConditions.length > 0) {
    filtered = filtered.filter(isProfileCompatible);
  }

  const handleOrder = async (recipe: RecipeResponse) => {
    setOrderingId(recipe.id);
    setOrderMsg(null);
    try {
      await orderRepository.placeOrder({ recipeId: recipe.id });
      setOrderMsg({ id: recipe.id, type: 'ok', text: 'Order placed!' });
    } catch (err) {
      const msg =
        axios.isAxiosError(err) && err.response?.data?.message
          ? err.response.data.message
          : 'Could not place order.';
      setOrderMsg({ id: recipe.id, type: 'err', text: msg });
    } finally {
      setOrderingId(null);
    }
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
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={!!orderingId}
                      onClick={() => handleOrder(recipe)}
                    >
                      {orderingId === recipe.id ? 'Ordering…' : 'Order Now'}
                    </button>
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
