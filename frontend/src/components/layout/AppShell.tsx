import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Doctor',
  DIETICIAN: 'Dietician',
  PATIENT: 'Patient',
  KITCHEN: 'Kitchen',
};

/** Persistent sidebar + content area shell wrapping all authenticated pages */
export function AppShell() {
  const { logout, credentials, role } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navLink = (to: string, label: string) => (
    <NavLink key={to} to={to} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
      {label}
    </NavLink>
  );

  return (
    <div className="app-shell">
      <nav className="sidebar">
        <div className="sidebar-brand">RecipeMaker</div>

        {credentials && (
          <div className="sidebar-user-block">
            <p className="sidebar-user">{credentials.username}</p>
            {role && <span className="sidebar-role-badge">{ROLE_LABELS[role] ?? role}</span>}
          </div>
        )}

        {navLink('/recipes', 'Menu')}

        {role === 'PATIENT' && (
          <>
            {navLink('/orders/active', 'My Orders')}
            {navLink('/orders/history', 'Order History')}
            {navLink('/profile', 'My Profile')}
          </>
        )}

        {(role === 'DOCTOR' || role === 'ADMIN') && navLink('/doctor', 'My Patients')}

        {(role === 'DIETICIAN' || role === 'ADMIN') && navLink('/components', 'Components')}

        {(role === 'DOCTOR' || role === 'DIETICIAN' || role === 'ADMIN') &&
          navLink('/conditions', 'Conditions')}

        {(role === 'KITCHEN' || role === 'ADMIN') && navLink('/kitchen', 'Kitchen')}

        <button className="nav-link nav-logout" onClick={handleLogout}>
          Logout
        </button>
      </nav>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
