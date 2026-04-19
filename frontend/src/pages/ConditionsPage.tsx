import { useState } from 'react';
import { conditionRepository } from '../api/conditions';
import { useConditions } from '../hooks/useConditions';
import { useAuth } from '../context/AuthContext';

export function ConditionsPage() {
  const { conditions, loading, error, refresh } = useConditions();
  const { role } = useAuth();
  const canManage = role === 'DOCTOR' || role === 'ADMIN';
  const [showForm, setShowForm] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [saving, setSaving] = useState(false);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await conditionRepository.create({ name: newName, description: newDescription });
      setNewName('');
      setNewDescription('');
      setShowForm(false);
      refresh();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Delete condition "${name}"?`)) return;
    await conditionRepository.delete(id);
    refresh();
  };

  if (loading) return <p className="page-loading">Loading…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>🩺 Patient Conditions</h2>
        {canManage && (
          <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancel' : '+ Add Condition'}
          </button>
        )}
      </div>

      {canManage && showForm && (
        <form className="form inline-form" onSubmit={handleCreate}>
          <div className="form-row">
            <div className="form-group">
              <label>Name *</label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>Description</label>
              <input
                type="text"
                value={newDescription}
                onChange={(e) => setNewDescription(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Creating…' : 'Create'}
          </button>
        </form>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Created By</th>
            {canManage && <th></th>}
          </tr>
        </thead>
        <tbody>
          {conditions.map((c) => (
            <tr key={c.id}>
              <td>
                {c.name}
                {c.createdByDoctorName && (
                  <span className="doctor-badge"> (Dr. {c.createdByDoctorName})</span>
                )}
              </td>
              <td>{c.description || '—'}</td>
              <td>{c.createdByDoctorName ? `Dr. ${c.createdByDoctorName}` : 'System'}</td>
              {canManage && (
                <td>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(c.id, c.name)}
                  >
                    Delete
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
