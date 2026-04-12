import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { recipeRepository } from '../api/recipes';
import { useComponents } from '../hooks/useComponents';
import { RecipeRequestBuilder } from '../patterns/recipeRequestBuilder';

/**
 * Shared create / edit form for recipes.
 *
 * Uses the Builder Pattern (RecipeRequestBuilder) to assemble and validate
 * the request payload before submitting, keeping validation logic out of
 * the component. The mainComponent dropdown is filtered to non-modifiable
 * components only; the multi-checkbox is filtered to modifiable ones.
 */
export function RecipeFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [mainComponentId, setMainComponentId] = useState<number | ''>('');
  const [selectedModifiable, setSelectedModifiable] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const { components } = useComponents();
  const mainComponents = components.filter((c) => !c.modifiable);
  const modifiableComponents = components.filter((c) => c.modifiable);

  // Pre-fill form when editing
  useEffect(() => {
    if (!isEdit) return;
    recipeRepository.getById(Number(id)).then((recipe) => {
      setName(recipe.name);
      setDescription(recipe.description ?? '');
      setMainComponentId(recipe.mainComponent.id);
      setSelectedModifiable(recipe.modifiableComponents.map((c) => c.id));
    });
  }, [id, isEdit]);

  const toggleModifiable = (cid: number) => {
    setSelectedModifiable((prev) =>
      prev.includes(cid) ? prev.filter((c) => c !== cid) : [...prev, cid]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);

    try {
      // Builder validates required fields and constructs the request object
      const request = new RecipeRequestBuilder()
        .name(name)
        .description(description)
        .mainComponent(Number(mainComponentId))
        .modifiableComponents(selectedModifiable)
        .build();

      if (isEdit) {
        await recipeRepository.update(Number(id), request);
      } else {
        await recipeRepository.create(request);
      }
      navigate('/recipes');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save recipe.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page">
      <h2>{isEdit ? 'Edit Recipe' : 'New Recipe'}</h2>

      <form className="form" onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Name *</label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label htmlFor="mainComponent">Main Component (non-modifiable) *</label>
          <select
            id="mainComponent"
            value={mainComponentId}
            onChange={(e) => setMainComponentId(Number(e.target.value))}
            required
          >
            <option value="">Select a main component…</option>
            {mainComponents.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Optional Components (modifiable)</label>
          <div className="checkbox-group">
            {modifiableComponents.map((c) => (
              <label key={c.id} className="checkbox-label">
                <input
                  type="checkbox"
                  checked={selectedModifiable.includes(c.id)}
                  onChange={() => toggleModifiable(c.id)}
                />
                {c.name}
              </label>
            ))}
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="form-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate(-1)}
          >
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Recipe'}
          </button>
        </div>
      </form>
    </div>
  );
}
