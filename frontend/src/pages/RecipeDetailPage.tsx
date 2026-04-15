import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { recipeRepository } from '../api/recipes';
import { usePatientProfile } from '../context/PatientProfileContext';
import { useAuth } from '../context/AuthContext';
import type { RecipeResponse } from '../types/api';

export function RecipeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [recipe, setRecipe] = useState<RecipeResponse | null>(null);
  const [loadingRecipe, setLoadingRecipe] = useState(true);

  const { profileConditions } = usePatientProfile();
  const { role } = useAuth();
  const canEdit = role === 'DIETICIAN' || role === 'ADMIN';

  useEffect(() => {
    if (!id) return;
    setLoadingRecipe(true);
    recipeRepository
      .getById(Number(id))
      .then(setRecipe)
      .catch(() => setRecipe(null))
      .finally(() => setLoadingRecipe(false));
  }, [id]);

  const handleDelete = async () => {
    if (!confirm(`Delete recipe "${recipe?.name}"?`)) return;
    await recipeRepository.delete(Number(id));
    navigate('/recipes');
  };

  if (loadingRecipe) return <p className="page-loading">Loading…</p>;
  if (!recipe) return <p className="error-msg">Recipe not found.</p>;

  const profileConditionNames = new Set(profileConditions.map((c) => c.name));

  // Gather all conflicts across all components against the user's profile
  const conflicts: { componentName: string; conditionName: string }[] = [];
  const allComponents = [recipe.mainComponent, ...recipe.modifiableComponents];
  for (const comp of allComponents) {
    for (const condName of comp.incompatibleConditionNames) {
      if (profileConditionNames.has(condName)) {
        conflicts.push({ componentName: comp.name, conditionName: condName });
      }
    }
  }
  const profileSafe = conflicts.length === 0;

  return (
    <div className="page">
      <div className="page-header">
        <h2>{recipe.name}</h2>
        {canEdit && (
          <div className="action-group">
            <Link to={`/recipes/${id}/edit`} className="btn btn-secondary">
              Edit
            </Link>
            <button onClick={handleDelete} className="btn btn-danger">
              Delete
            </button>
          </div>
        )}
      </div>

      <div className="recipe-detail-badges">
        {recipe.mealCourse && (
          <span className={`meal-course-badge course-${recipe.mealCourse.toLowerCase()}`}>
            {recipe.mealCourse}
          </span>
        )}
        {recipe.mealType && (
          <span className={`meal-type-badge type-${recipe.mealType.toLowerCase()}`}>
            {recipe.mealType}
          </span>
        )}
      </div>

      {recipe.description && <p className="recipe-description">{recipe.description}</p>}

      {/* Profile compatibility summary — only shown when the user has a saved profile */}
      {profileConditions.length > 0 && (
        <div className={`compat-result ${profileSafe ? 'compat-ok' : 'compat-warn'}`}>
          <div className="compat-header">
            <strong>{profileSafe ? '✓ Compatible with your profile' : '✗ Conflicts with your profile'}</strong>
          </div>
          {!profileSafe && (
            <ul className="compat-conflict-list">
              {conflicts.map((cf, i) => (
                <li key={i}>
                  <strong>{cf.componentName}</strong> is incompatible with <em>{cf.conditionName}</em>
                </li>
              ))}
            </ul>
          )}
          <p className="compat-reason">
            Profile: {profileConditions.map((c) => c.name).join(', ')}
          </p>
        </div>
      )}

      {/* Main component */}
      <section className="detail-section">
        <h3>Main Component</h3>
        <div className="component-item component-main">
          <strong>{recipe.mainComponent.name}</strong>
          {recipe.mainComponent.description && (
            <p>{recipe.mainComponent.description}</p>
          )}
          {recipe.mainComponent.incompatibleConditionNames.length > 0 && (
            <p className="incompatible-hint">
              Incompatible with:{' '}
              {recipe.mainComponent.incompatibleConditionNames.join(', ')}
            </p>
          )}
        </div>
      </section>

      {/* Optional components */}
      {recipe.modifiableComponents.length > 0 && (
        <section className="detail-section">
          <h3>Optional Components</h3>
          <div className="component-list">
            {recipe.modifiableComponents.map((c) => (
              <div key={c.id} className="component-item">
                <strong>{c.name}</strong>
                {c.description && <p>{c.description}</p>}
                {c.incompatibleConditionNames.length > 0 && (
                  <p className="incompatible-hint">
                    Incompatible with: {c.incompatibleConditionNames.join(', ')}
                  </p>
                )}
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
