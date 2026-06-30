import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { NOTIFICATIONS } from '../data/mockData';
import {
  LayoutDashboard, FileText, Map, Building2, Bot, AlertTriangle, Mic,
  BarChart3, Bell, Settings, LogOut, ShieldCheck, Zap
} from 'lucide-react';

const NAV = [
  { label: 'Overview', icon: LayoutDashboard, to: '/dashboard' },
  { label: 'Reports', icon: FileText, to: '/reports' },
  { label: 'Voice Intake', icon: Mic, to: '/voice-intake' },
  { label: 'Map View', icon: Map, to: '/map' },
  { label: 'Departments', icon: Building2, to: '/departments' },
  { label: 'AI Agents', icon: Bot, to: '/agents' },
  { label: 'Escalations', icon: AlertTriangle, to: '/escalations', badge: NOTIFICATIONS.filter(n => n.type === 'escalation' && !n.read).length },
  { label: 'Analytics', icon: BarChart3, to: '/analytics' },
  { label: 'Notifications', icon: Bell, to: '/notifications', badge: NOTIFICATIONS.filter(n => !n.read).length },
  { label: 'Settings', icon: Settings, to: '/settings' },
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 32, height: 32, background: 'linear-gradient(135deg, #38bdf8, #3b82f6)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <ShieldCheck size={18} color="#0f172a" strokeWidth={2.5} />
          </div>
          <div>
            <div className="sidebar-logo-title">NagarNetra</div>
            <div className="sidebar-logo-sub">Authority Panel</div>
          </div>
        </div>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section">Navigation</div>
        {NAV.map(({ label, icon: Icon, to, badge }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <Icon size={18} />
            <span style={{ flex: 1 }}>{label}</span>
            {badge ? <span className="nav-badge">{badge}</span> : null}
          </NavLink>
        ))}

        <div className="nav-section" style={{ marginTop: 8 }}>System</div>
        <div style={{ padding: '8px 16px' }}>
          <div style={{ background: 'rgba(16,185,129,0.08)', border: '1px solid rgba(16,185,129,0.2)', borderRadius: 8, padding: '10px 12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
              <Zap size={12} color="#10b981" />
              <span style={{ fontSize: 11, color: '#10b981', fontWeight: 700 }}>SYSTEM LIVE</span>
            </div>
            <div style={{ fontSize: 10, color: 'var(--text3)' }}>8 AI Agents Active</div>
            <div style={{ fontSize: 10, color: 'var(--text3)' }}>390 Total Reports</div>
          </div>
        </div>
      </nav>

      <div className="sidebar-footer">
        <div className="user-card">
          <div className="user-avatar">{user?.avatar || 'AU'}</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="user-name truncate">{user?.name || 'Authority'}</div>
            <div className="user-role">{user?.role}</div>
          </div>
          <button
            onClick={handleLogout}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text3)', padding: 4 }}
            title="Logout"
          >
            <LogOut size={16} />
          </button>
        </div>
      </div>
    </aside>
  );
}

