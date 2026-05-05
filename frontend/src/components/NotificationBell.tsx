import { useState, useRef, useEffect } from 'react';
import { useNotifications } from '../context/NotificationContext';

/** Bell icon SVG */
function BellIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className="notif-bell-icon">
      <path d="M10 2a6 6 0 00-6 6v3l-1.5 2.5h15L16 11V8a6 6 0 00-6-6z" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 16.5a2 2 0 004 0" strokeLinecap="round" />
    </svg>
  );
}

/**
 * Notification bell button with unread badge.
 * Clicking it opens a dropdown panel listing all notifications newest-first.
 */
export function NotificationBell() {
  const { notifications, unreadCount, markRead, markAllRead } = useNotifications();
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  // Close panel on outside click
  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  return (
    <div className="notif-bell-wrapper" ref={panelRef}>
      <button
        className="notif-bell-btn"
        aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
        onClick={() => setOpen((v) => !v)}
      >
        <BellIcon />
        {unreadCount > 0 && (
          <span className="notif-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
      </button>

      {open && (
        <div className="notif-panel">
          <div className="notif-panel-header">
            <span className="notif-panel-title">Notifications</span>
            {unreadCount > 0 && (
              <button
                className="notif-mark-all-btn"
                onClick={() => markAllRead()}
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="notif-list">
            {notifications.length === 0 ? (
              <p className="notif-empty">No notifications yet.</p>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  className={`notif-item${n.read ? ' notif-item--read' : ''}`}
                  onClick={() => { if (!n.read) markRead(n.id); }}
                >
                  <div className="notif-item-msg">{n.message}</div>
                  <div className="notif-item-meta">
                    <span className={`notif-type-badge notif-type-${n.type.toLowerCase()}`}>
                      {n.type === 'ORDER_PLACED' ? 'New order' : 'Status update'}
                    </span>
                    <span className="notif-item-time">
                      {new Date(n.createdAt).toLocaleString()}
                    </span>
                  </div>
                  {!n.read && <span className="notif-unread-dot" aria-hidden="true" />}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
