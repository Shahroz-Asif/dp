interface Props {
  selectable: boolean;
}

/** Visual badge indicating recipe / component compatibility status */
export function StatusBadge({ selectable }: Props) {
  return (
    <span className={selectable ? 'badge badge-ok' : 'badge badge-warn'}>
      {selectable ? 'Compatible' : 'Incompatible'}
    </span>
  );
}
