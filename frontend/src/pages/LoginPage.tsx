import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { RegisterModal } from '../components/RegisterModal';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [showRegister, setShowRegister] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleRegisterSuccess = async (username: string, password: string) => {
    setShowRegister(false);
    try {
      await login(username, password);
      navigate('/recipes');
    } catch {
      setError('Account created! Please sign in.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(username, password);
      navigate('/recipes');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid username or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Hero panel */}
      <div className="login-page-hero">
        <div className="login-hero-icon">🥗</div>
        <h1 className="login-hero-title">RecipeMaker</h1>
        <p className="login-hero-subtitle">
          Personalised hospital meal planning — tailored to every patient's dietary needs.
        </p>
      </div>

      {/* Form panel */}
      <div className="login-panel">
        <div className="login-card">
          <div className="login-card-header">
            <h2 className="login-title">Welcome back</h2>
            <p className="login-subtitle">Sign in to your account to continue</p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
              />
            </div>

            {error && <p className="error-msg">{error}</p>}

            <button
              type="submit"
              className="btn btn-primary btn-full"
              disabled={loading}
              style={{ marginTop: '0.5rem', padding: '0.7rem 1.1rem', fontSize: '0.95rem' }}
            >
              {loading ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <div className="login-register-row">
            <span>New here?</span>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => { setError(null); setShowRegister(true); }}
            >
              Create Account
            </button>
          </div>

          <div className="login-demo-accounts">
            <p className="login-hint">Demo accounts:</p>
            <table className="login-demo-table">
              <tbody>
                <tr><td>admin / admin</td><td className="login-demo-role">Admin</td></tr>
                <tr><td>doctor1 / doctor</td><td className="login-demo-role">Doctor</td></tr>
                <tr><td>dietician1 / dietician</td><td className="login-demo-role">Dietician</td></tr>
                <tr><td>patient1 / patient</td><td className="login-demo-role">Patient</td></tr>
                <tr><td>kitchen / kitchen</td><td className="login-demo-role">Kitchen</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {showRegister && (
        <RegisterModal
          onClose={() => setShowRegister(false)}
          onSuccess={handleRegisterSuccess}
        />
      )}
    </div>
  );
}
