import { useState } from 'react';
import { useConditions } from '../hooks/useConditions';
import { usePatientProfile } from '../context/PatientProfileContext';

/**
 * Patient Profile setup page.
 *
 * Lets the authenticated user select which dietary/medical conditions apply
 * to them. Selections are saved to localStorage via PatientProfileContext
 * and used across the app (e.g. recipe list filtering) without requiring
 * a backend patient entity.
 */
export function PatientProfilePage() {
  const { conditions, loading, error } = useConditions();
  const { profileConditions, setProfileConditions } = usePatientProfile();

  // Local checkbox state, seeded from the persisted profile
  const [selected, setSelected] = useState<Set<number>>(
    () => new Set(profileConditions.map((c) => c.id))
  );
  const [saved, setSaved] = useState(false);

  const toggle = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
    setSaved(false);
  };

  const handleSave = () => {
    const picked = conditions
      .filter((c) => selected.has(c.id))
      .map(({ id, name }) => ({ id, name }));
    setProfileConditions(picked);
    setSaved(true);
  };

  const handleClear = () => {
    setSelected(new Set());
    setSaved(false);
  };

  if (loading) return <p className="page-loading">Loading conditions…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>My Health Profile</h2>
      </div>

      <p className="profile-intro">
        Select the dietary or medical conditions that apply to you. Recipes will be
        filterable by compatibility with your profile.
      </p>

      {conditions.length === 0 ? (
        <p className="empty-state">
          No conditions available yet. An admin can add them on the Conditions page.
        </p>
      ) : (
        <div className="profile-conditions-list">
          {conditions.map((c) => (
            <label
              key={c.id}
              className={`profile-condition-item${selected.has(c.id) ? ' profile-condition-item--selected' : ''}`}
            >
              <input
                type="checkbox"
                checked={selected.has(c.id)}
                onChange={() => toggle(c.id)}
              />
              <div className="profile-condition-body">
                <span className="profile-condition-name">{c.name}</span>
                {c.description && (
                  <span className="profile-condition-desc">{c.description}</span>
                )}
              </div>
            </label>
          ))}
        </div>
      )}

      <div className="profile-actions">
        <button className="btn btn-primary" onClick={handleSave} disabled={conditions.length === 0}>
          Save Profile
        </button>
        {selected.size > 0 && (
          <button className="btn btn-secondary" onClick={handleClear}>
            Clear All
          </button>
        )}
        {saved && (
          <span className="profile-saved-msg">
            ✓ Profile saved ({profileConditions.length} condition{profileConditions.length !== 1 ? 's' : ''})
          </span>
        )}
      </div>
    </div>
  );
}
