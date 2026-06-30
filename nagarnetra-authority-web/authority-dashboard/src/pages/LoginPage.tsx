import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import type { UserRole } from '../types';
import { ShieldCheck, Eye, EyeOff, AlertCircle } from 'lucide-react';

export default function LoginPage() {
  const [email, setEmail] = useState('admin@nagarnetra.gov.in');
  const [password, setPassword] = useState('admin123');
  const [role, setRole] = useState<UserRole>('Super Admin');
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) { setError('Please fill all fields'); return; }
    setLoading(true);
    setError('');
    const ok = await login(email, password, role);
    setLoading(false);
    if (ok) navigate('/dashboard');
    else setError('Invalid credentials. Please try again.');
  };

  const demoLogin = (r: UserRole, e: string) => { setRole(r); setEmail(e); setPassword('demo123'); };

  return (
    <div className="login-page">
      <div className="login-card fade-in">
        <div className="login-logo">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 12 }}>
            <div style={{ width: 56, height: 56, background: 'linear-gradient(135deg, #38bdf8, #3b82f6)', borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ShieldCheck size={28} color="#0f172a" strokeWidth={2.5} />
            </div>
          </div>
          <h1>NagarNetra AI</h1>
          <p>Authority Control Panel — Government of Karnataka</p>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="input-group">
            <label className="input-label">Government Email ID</label>
            <input className="input" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="name@nagarnetra.gov.in" required />
          </div>
          <div className="input-group">
            <label className="input-label">Password</label>
            <div style={{ position: 'relative' }}>
              <input className="input" type={showPass ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" style={{ paddingRight: 40 }} required />
              <button type="button" onClick={() => setShowPass(!showPass)} style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text3)' }}>
                {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>
          <div className="input-group">
            <label className="input-label">Role</label>
            <select className="input" value={role} onChange={e => setRole(e.target.value as UserRole)}>
              <option>Super Admin</option>
              <option>Department Admin</option>
              <option>Field Officer</option>
            </select>
          </div>

          {error && (
            <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 8, padding: '10px 14px', color: 'var(--red)', fontSize: 13, display: 'flex', alignItems: 'center', gap: 8 }}>
              <AlertCircle size={15} /> {error}
            </div>
          )}

          <button className="btn btn-primary btn-lg w-full" type="submit" disabled={loading} style={{ marginTop: 4, justifyContent: 'center' }}>
            {loading ? 'Authenticating…' : 'Sign In to Authority Panel'}
          </button>
        </form>

        <div style={{ marginTop: 24, borderTop: '1px solid var(--border)', paddingTop: 16 }}>
          <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 8, textAlign: 'center' }}>DEMO ACCOUNTS</div>
          <div style={{ display: 'flex', gap: 8, flexDirection: 'column' }}>
            {([
              ['Super Admin', 'admin@nagarnetra.gov.in'],
              ['Department Admin', 'bbmp@nagarnetra.gov.in'],
              ['Field Officer', 'officer@nagarnetra.gov.in'],
            ] as [UserRole, string][]).map(([r, e]) => (
              <button key={r} className="btn btn-ghost btn-sm" onClick={() => demoLogin(r, e)} style={{ justifyContent: 'flex-start', gap: 8 }}>
                <span style={{ fontSize: 10, background: 'var(--card2)', padding: '2px 6px', borderRadius: 4, color: 'var(--accent)' }}>{r}</span>
                <span style={{ color: 'var(--text3)', fontSize: 11 }}>{e}</span>
              </button>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 16, textAlign: 'center', fontSize: 11, color: 'var(--text3)' }}>
          🔒 Secured Government System — Unauthorized access is prohibited
        </div>
      </div>
    </div>
  );
}
