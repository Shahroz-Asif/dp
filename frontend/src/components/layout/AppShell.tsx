import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { usePatientProfile } from '../../context/PatientProfileContext';

/** Persistent sidebar + content area shell wrapping all authenticated pages */
export function AppShell() {
  const { logout, credentials } = useAuth();
  const { profileConditions } = usePatientProfile();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <nav className="sidebar">
        <div className="sidebar-brand">RecipeMaker</div>

        {credentials && (
          <p className="sidebar-user">{credentials.username}</p>
        )}

        <NavLink to="/recipes" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          Recipes
        </NavLink>
        <NavLink to="/components" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          Components
        </NavLink>
        <NavLink to="/conditions" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          Conditions
        </NavLink>
        <NavLink to="/profile" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          My Profile
          {profileConditions.length > 0 && (
            <span className="profile-nav-badge">{profileConditions.length}</span>
          )}
        </NavLink>

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
