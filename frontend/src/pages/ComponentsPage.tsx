import { useState } from 'react';
import { componentRepository } from '../api/components';
import { useComponents } from '../hooks/useComponents';

export function ComponentsPage() {
  const { components, loading, error, refresh } = useComponents();
  const [showForm, setShowForm] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newModifiable, setNewModifiable] = useState(true);
  const [saving, setSaving] = useState(false);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await componentRepository.create({
        type: newModifiable ? 'MODIFIABLE' : 'NON_MODIFIABLE',
        name: newName,
        description: newDescription,
        incompatibleConditions: [],
      });
      setNewName('');
      setNewDescription('');
      setShowForm(false);
      refresh();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Delete component "${name}"?`)) return;
    await componentRepository.delete(id);
    refresh();
  };

  if (loading) return <p className="page-loading">Loading…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Components</h2>
        <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
          {showForm ? 'Cancel' : '+ Add Component'}
        </button>
      </div>

      {showForm && (
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
          <div className="form-group">
            <label className="checkbox-label inline">
              <input
                type="checkbox"
                checked={newModifiable}
                onChange={(e) => setNewModifiable(e.target.checked)}
              />
              Modifiable (optional component)
            </label>
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
            <th>Type</th>
            <th>Incompatible With</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {components.map((c) => (
            <tr key={c.id}>
              <td>{c.name}</td>
              <td>{c.description || '—'}</td>
              <td>
                <span className={`type-badge ${c.modifiable ? 'type-optional' : 'type-main'}`}>
                  {c.modifiable ? 'Modifiable' : 'Non-Modifiable'}
                </span>
              </td>
              <td>
                {c.incompatibleConditions.length > 0
                  ? c.incompatibleConditions.map((ic: { name: string }) => ic.name).join(', ')
                  : '—'}
              </td>
              <td>
                <button
                  className="btn btn-sm btn-danger"
                  onClick={() => handleDelete(c.id, c.name)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
