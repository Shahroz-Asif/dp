import { useState } from 'react';
import type { ComponentResponse, RecipeResponse } from '../types/api';

interface Props {
  recipe: RecipeResponse;
  /** Condition names the current patient has — used to disable incompatible components */
  patientConditionNames: Set<string>;
  onConfirm: (selectedComponentIds: number[]) => void;
  onCancel: () => void;
  submitting: boolean;
}

function isIncompatible(comp: ComponentResponse, patientConditionNames: Set<string>): boolean {
  return comp.incompatibleConditionNames.some((name) => patientConditionNames.has(name));
}

/**
 * Modal shown when a patient clicks "Order Now".
 *
 * Displays the main component (always included) and optional add-ons as
 * checkboxes.  Optional components that conflict with the patient's registered
 * conditions are shown disabled with a tooltip so the patient understands why.
 */
export function PlaceOrderModal({ recipe, patientConditionNames, onConfirm, onCancel, submitting }: Props) {
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const toggle = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const hasOptional = recipe.modifiableComponents.length > 0;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Customise your order</h3>
          <p className="modal-subtitle">{recipe.name}</p>
        </div>

        {/* Main component — always included, non-removable */}
        <div className="modal-section">
          <h4 className="modal-section-title">Main Component</h4>
          <div className="modal-component modal-component--main">
            <span className="modal-component-check">✓</span>
            <span className="modal-component-name">{recipe.mainComponent.name}</span>
            <span className="modal-tag modal-tag--included">Included</span>
          </div>
        </div>

        {/* Optional add-ons */}
        {hasOptional && (
          <div className="modal-section">
            <h4 className="modal-section-title">Optional Add-ons</h4>
            <div className="modal-component-list">
              {recipe.modifiableComponents.map((comp) => {
                const incompat = isIncompatible(comp, patientConditionNames);
                const conflictingConds = comp.incompatibleConditionNames
                  .filter((n) => patientConditionNames.has(n))
                  .join(', ');

                return (
                  <label
                    key={comp.id}
                    className={`modal-component modal-component--optional${incompat ? ' modal-component--incompatible' : ''}`}
                    title={incompat ? `Not available: conflicts with ${conflictingConds}` : comp.description || undefined}
                  >
                    <input
                      type="checkbox"
                      checked={selected.has(comp.id) && !incompat}
                      disabled={incompat || submitting}
                      onChange={() => toggle(comp.id)}
                    />
                    <span className="modal-component-name">{comp.name}</span>
                    {incompat
                      ? <span className="modal-tag modal-tag--incompat">⚠ {conflictingConds}</span>
                      : selected.has(comp.id) && <span className="modal-tag modal-tag--selected">Added</span>
                    }
                  </label>
                );
              })}
            </div>
          </div>
        )}

        {!hasOptional && (
          <p className="modal-no-optional">No optional add-ons available for this recipe.</p>
        )}

        <div className="modal-actions">
          <button className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={() => onConfirm(Array.from(selected))}
            disabled={submitting}
          >
            {submitting ? 'Placing…' : `Place Order${selected.size > 0 ? ` (+${selected.size})` : ''}`}
          </button>
        </div>
      </div>
    </div>
  );
}
