import { useEffect, useState } from 'react';
import { useConditions } from '../hooks/useConditions';
import { usePatientProfile } from '../context/PatientProfileContext';
import { useAuth } from '../context/AuthContext';
import { patientRepository } from '../api/patients';
import type { PatientProfileResponse } from '../types/api';

export function PatientProfilePage() {
  const { role } = useAuth();
  const { conditions, loading: condLoading, error: condError } = useConditions();
  const { profileConditions, setProfileConditions } = usePatientProfile();

  // Backend profile — used when role is PATIENT
  const [profile, setProfile] = useState<PatientProfileResponse | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);

  // Local checkbox state for non-PATIENT users (manual filter tool)
  const [selected, setSelected] = useState<Set<number>>(
    () => new Set(profileConditions.map((c) => c.id))
  );
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (role !== 'PATIENT') return;
    setProfileLoading(true);
    patientRepository
      .getMyProfile()
      .then((p) => {
        setProfile(p);
        // Sync backend conditions into context for recipe list filtering
        setProfileConditions(p.conditions.map((c) => ({ id: c.id, name: c.name })));
      })
      .catch(() => setProfileError('Failed to load your profile.'))
      .finally(() => setProfileLoading(false));
  }, [role, setProfileConditions]);

  // ── PATIENT view: read-only backend profile ──────────────────────────────
  if (role === 'PATIENT') {
    if (profileLoading) return <p className="page-loading">Loading your profile…</p>;
    if (profileError) return <p className="error-msg">{profileError}</p>;
    if (!profile) return <p className="page-loading">Loading…</p>;

    return (
      <div className="page">
        <div className="page-header">
          <h2>My Health Profile</h2>
        </div>

        <div className="profile-info-card">
          <div className="profile-info-row">
            <span className="profile-info-label">Name</span>
            <span className="profile-info-value">{profile.name}</span>
          </div>
          <div className="profile-info-row">
            <span className="profile-info-label">Age</span>
            <span className="profile-info-value">{profile.age}</span>
          </div>
          {profile.assignedDoctorUsername && (
            <div className="profile-info-row">
              <span className="profile-info-label">Assigned Doctor</span>
              <span className="profile-info-value">{profile.assignedDoctorUsername}</span>
            </div>
          )}
          {profile.notes && (
            <div className="profile-info-row">
              <span className="profile-info-label">Notes</span>
              <span className="profile-info-value">{profile.notes}</span>
            </div>
          )}
        </div>

        <h3 className="profile-section-title">My Conditions</h3>
        {profile.conditions.length === 0 ? (
          <p className="empty-state">No conditions assigned to you yet. Contact your doctor.</p>
        ) : (
          <div className="profile-conditions-list">
            {profile.conditions.map((c) => (
              <div key={c.id} className="profile-condition-item profile-condition-item--selected">
                <div className="profile-condition-body">
                  <span className="profile-condition-name">{c.name}</span>
                  {c.description && (
                    <span className="profile-condition-desc">{c.description}</span>
                  )}
                  {c.createdByDoctorName && (
                    <span className="doctor-badge">Dr. {c.createdByDoctorName}</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
        <p className="profile-intro" style={{ marginTop: '1rem' }}>
          Your conditions are managed by your assigned doctor. Recipes on the menu will show
          compatibility with your profile.
        </p>
      </div>
    );
  }

  // ── Non-PATIENT view: manual condition selector (filter tool) ────────────
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

  if (condLoading) return <p className="page-loading">Loading conditions…</p>;
  if (condError) return <p className="error-msg">{condError}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Profile Filter</h2>
      </div>

      <p className="profile-intro">
        Select conditions to filter recipes by compatibility on the menu page.
      </p>

      {conditions.length === 0 ? (
        <p className="empty-state">No conditions available yet.</p>
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
          Save Filter
        </button>
        {selected.size > 0 && (
          <button className="btn btn-secondary" onClick={handleClear}>
            Clear All
          </button>
        )}
        {saved && (
          <span className="profile-saved-msg">
            ✓ Filter saved ({profileConditions.length} condition{profileConditions.length !== 1 ? 's' : ''})
          </span>
        )}
      </div>
    </div>
  );
}
