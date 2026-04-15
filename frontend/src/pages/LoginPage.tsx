import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

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
      <div className="login-card">
        <h1 className="login-title">RecipeMaker</h1>
        <p className="login-subtitle">Hospital Meal System</p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
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
              required
            />
          </div>

          {error && <p className="error-msg">{error}</p>}

          <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

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
  );
}
