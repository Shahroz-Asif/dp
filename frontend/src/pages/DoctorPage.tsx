import { useCallback, useEffect, useState } from 'react';
import { doctorRepository } from '../api/doctor';
import { conditionRepository } from '../api/conditions';
import type { PatientCondition, PatientProfileResponse } from '../types/api';

export function DoctorPage() {
  const [patients, setPatients] = useState<PatientProfileResponse[]>([]);
  const [allConditions, setAllConditions] = useState<PatientCondition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedPatient, setExpandedPatient] = useState<number | null>(null);
  const [assigning, setAssigning] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [pats, conds] = await Promise.all([
        doctorRepository.getMyPatients(),
        conditionRepository.getAll(),
      ]);
      setPatients(pats);
      setAllConditions(conds);
    } catch {
      setError('Failed to load data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleAssign = async (patientId: number, conditionId: number) => {
    setAssigning(true);
    try {
      await doctorRepository.assignCondition(patientId, conditionId);
      await load();
    } catch {
      setError('Failed to assign condition.');
    } finally {
      setAssigning(false);
    }
  };

  const handleRemove = async (patientId: number, conditionId: number) => {
    setAssigning(true);
    try {
      await doctorRepository.removeCondition(patientId, conditionId);
      await load();
    } catch {
      setError('Failed to remove condition.');
    } finally {
      setAssigning(false);
    }
  };

  if (loading) return <p className="page-loading">Loading patients…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>👥 My Patients</h2>
      </div>

      {patients.length === 0 ? (
        <p className="empty-state">No patients assigned to you yet.</p>
      ) : (
        <div className="patient-list">
          {patients.map((patient) => {
            const isExpanded = expandedPatient === patient.id;
            const assignedIds = new Set(patient.conditions.map((c) => c.id));
            const available = allConditions.filter((c) => !assignedIds.has(c.id));

            return (
              <div key={patient.id} className="patient-card">
                <div
                  className="patient-card-header"
                  onClick={() => setExpandedPatient(isExpanded ? null : patient.id)}
                >
                  <div>
                    <h3 className="patient-name">{patient.name}</h3>
                    <p className="patient-meta">
                      Age: {patient.age} &middot; {patient.conditions.length} condition(s)
                    </p>
                  </div>
                  <span className="expand-toggle">{isExpanded ? '▲' : '▼'}</span>
                </div>

                {isExpanded && (
                  <div className="patient-card-body">
                    <h4>Assigned Conditions</h4>
                    {patient.conditions.length === 0 ? (
                      <p className="muted-text">No conditions assigned.</p>
                    ) : (
                      <ul className="condition-chip-list">
                        {patient.conditions.map((c) => (
                          <li key={c.id} className="condition-chip">
                            <span>{c.name}</span>
                            <button
                              className="btn btn-sm btn-danger"
                              disabled={assigning}
                              onClick={() => handleRemove(patient.id, c.id)}
                            >
                              ×
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}

                    {available.length > 0 && (
                      <div className="assign-condition-row">
                        <select
                          disabled={assigning}
                          defaultValue=""
                          onChange={(e) => {
                            const condId = Number(e.target.value);
                            if (condId) handleAssign(patient.id, condId);
                            e.currentTarget.value = '';
                          }}
                        >
                          <option value="">+ Assign a condition…</option>
                          {available.map((c) => (
                            <option key={c.id} value={c.id}>
                              {c.name}
                            </option>
                          ))}
                        </select>
                      </div>
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
