import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { NotificationBell } from '../NotificationBell';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Doctor',
  DIETICIAN: 'Dietician',
  PATIENT: 'Patient',
  KITCHEN: 'Kitchen',
};

// SVG icon components for sidebar navigation
const Icons = {
  Menu: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M3 5h14M3 10h14M3 15h14" strokeLinecap="round"/>
    </svg>
  ),
  Orders: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <rect x="3" y="3" width="14" height="14" rx="2"/>
      <path d="M7 7h6M7 10h6M7 13h4" strokeLinecap="round"/>
    </svg>
  ),
  History: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="10" cy="10" r="7"/>
      <path d="M10 6v4l2.5 2.5" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Profile: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="10" cy="7" r="3"/>
      <path d="M4 17c0-3.314 2.686-6 6-6s6 2.686 6 6" strokeLinecap="round"/>
    </svg>
  ),
  Patients: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="7" cy="7" r="2.5"/>
      <path d="M2 17c0-2.76 2.24-5 5-5s5 2.24 5 5" strokeLinecap="round"/>
      <circle cx="14" cy="7" r="2.5"/>
      <path d="M12 17c.3-2.6 2.3-4.6 5-5" strokeLinecap="round"/>
    </svg>
  ),
  Components: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <rect x="3" y="3" width="6" height="6" rx="1"/>
      <rect x="11" y="3" width="6" height="6" rx="1"/>
      <rect x="3" y="11" width="6" height="6" rx="1"/>
      <rect x="11" y="11" width="6" height="6" rx="1"/>
    </svg>
  ),
  Conditions: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M10 3C6.686 3 4 5.686 4 9c0 2.04.968 3.856 2.469 5.018L10 17l3.531-2.982A6.965 6.965 0 0016 9c0-3.314-2.686-6-6-6z"/>
    </svg>
  ),
  Kitchen: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M6 3v5a4 4 0 004 4 4 4 0 004-4V3" strokeLinecap="round"/>
      <path d="M10 12v5M7 17h6" strokeLinecap="round"/>
    </svg>
  ),
  Logout: () => (
    <svg className="nav-link-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M13 10H3m0 0l3-3m-3 3l3 3" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M8 5V4a1 1 0 011-1h6a1 1 0 011 1v12a1 1 0 01-1 1H9a1 1 0 01-1-1v-1" strokeLinecap="round"/>
    </svg>
  ),
};

/** Persistent sidebar + content area shell wrapping all authenticated pages */
export function AppShell() {
  const { logout, credentials, role } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navLink = (to: string, label: string, Icon: () => JSX.Element) => (
    <NavLink key={to} to={to} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
      <Icon />
      {label}
    </NavLink>
  );

  return (
    <div className="app-shell">
      <nav className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon">🥗</div>
          RecipeMaker
        </div>

        {credentials && (
          <div className="sidebar-user-block">
            <p className="sidebar-user">{credentials.username}</p>
            {role && <span className="sidebar-role-badge">{ROLE_LABELS[role] ?? role}</span>}
          </div>
        )}

        <div className="sidebar-nav-section">
          {navLink('/recipes', 'Menu', Icons.Menu)}

          {role === 'PATIENT' && (
            <>
              {navLink('/orders/active', 'My Orders', Icons.Orders)}
              {navLink('/orders/history', 'Order History', Icons.History)}
              {navLink('/profile', 'My Profile', Icons.Profile)}
            </>
          )}

          {(role === 'DOCTOR' || role === 'ADMIN') && navLink('/doctor', 'My Patients', Icons.Patients)}

          {(role === 'DIETICIAN' || role === 'ADMIN') && navLink('/components', 'Components', Icons.Components)}

          {(role === 'DOCTOR' || role === 'DIETICIAN' || role === 'ADMIN') &&
            navLink('/conditions', 'Conditions', Icons.Conditions)}

          {(role === 'KITCHEN' || role === 'ADMIN') && navLink('/kitchen', 'Kitchen', Icons.Kitchen)}
        </div>

        <div style={{ padding: '0 0.75rem' }}>
          <button className="nav-link nav-logout" onClick={handleLogout}>
            <Icons.Logout />
            Logout
          </button>
        </div>
      </nav>

      <main className="main-content">
        <div className="main-content-topbar">
          <NotificationBell />
        </div>
        <Outlet />
      </main>
    </div>
  );
}
