import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useRecipes } from '../hooks/useRecipes';
import { buildAndStrategy } from '../patterns/searchStrategy';
import { usePatientProfile } from '../context/PatientProfileContext';
import { useAuth } from '../context/AuthContext';
import { orderRepository } from '../api/orders';
import { PlaceOrderModal } from '../components/PlaceOrderModal';
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
  const [orderMsg, setOrderMsg] = useState<{ id: number; type: 'ok' | 'err'; text: string } | null>(null);
  const [orderModal, setOrderModal] = useState<RecipeResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const strategy = buildAndStrategy({ name: nameFilter, componentName: componentFilter });
  const profileConditionNames = new Set(profileConditions.map((c) => c.name));

  /** Full recipe incompatibility: any component (main or optional) conflicts. Used for the badge. */
  const isProfileCompatible = (recipe: RecipeResponse) => {
    const allComponents = [recipe.mainComponent, ...recipe.modifiableComponents];
    return allComponents.every((comp) =>
      comp.incompatibleConditionNames.every((condName) => !profileConditionNames.has(condName))
    );
  };

  /** Only the main component matters for whether the order can be placed at all. */
  const isMainCompatible = (recipe: RecipeResponse) =>
    recipe.mainComponent.incompatibleConditionNames.every(
      (condName) => !profileConditionNames.has(condName)
    );

  let filtered = recipes
    .filter((r) => courseFilter === 'ALL' || r.mealCourse === courseFilter)
    .filter((r) => strategy.matches(r));

  if (profileFilterOn && profileConditions.length > 0) {
    filtered = filtered.filter(isMainCompatible);
  }

  const handleOrder = async (selectedComponentIds: number[]) => {
    if (!orderModal) return;
    setSubmitting(true);
    try {
      await orderRepository.placeOrder({ recipeId: orderModal.id, selectedComponentIds });
      setOrderMsg({ id: orderModal.id, type: 'ok', text: 'Order placed!' });
      setOrderModal(null);
    } catch (err) {
      const msg =
        axios.isAxiosError(err) && err.response?.data?.message
          ? err.response.data.message
          : 'Could not place order.';
      setOrderMsg({ id: orderModal.id, type: 'err', text: msg });
      setOrderModal(null);
    } finally {
      setSubmitting(false);
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
            const canOrder = profileConditions.length === 0 || isMainCompatible(recipe);
            const thisOrderMsg = orderMsg?.id === recipe.id ? orderMsg : null;
            const courseIcon = recipe.mealCourse ? COURSE_ICONS[recipe.mealCourse] : '🍽️';

            return (
              <div key={recipe.id} className="recipe-card recipe-card-block">
                {/* Recipe image */}
                {recipe.imageUrl && (
                  <div className="recipe-card-image">
                    <img src={recipe.imageUrl} alt={recipe.name} loading="lazy" />
                  </div>
                )}
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
                    {!canOrder && profileConditions.length > 0 && (
                      <p className="order-error">Cannot order — main dish conflicts with your profile.</p>
                    )}
                    {!compatible && canOrder && profileConditions.length > 0 && (
                      <p className="order-warn">Some optional add-ons are unavailable for your profile.</p>
                    )}
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={!canOrder && profileConditions.length > 0}
                      title={!canOrder && profileConditions.length > 0 ? 'Main ingredient conflicts with your medical profile' : undefined}
                      onClick={() => { setOrderMsg(null); setOrderModal(recipe); }}
                    >
                      Order Now
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {orderModal && (
        <PlaceOrderModal
          recipe={orderModal}
          patientConditionNames={profileConditionNames}
          onConfirm={handleOrder}
          onCancel={() => setOrderModal(null)}
          submitting={submitting}
        />
      )}
    </div>
  );
}
