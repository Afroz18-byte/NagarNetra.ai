import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { NOTIFICATIONS } from '../data/mockData';
import { Sun, Moon, Bell, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface HeaderProps {
  title: string;
  subtitle?: string;
}

export default function Header({ title, subtitle }: HeaderProps) {
  const { isDark, toggleTheme } = useTheme();
  const { user } = useAuth();
  const navigate = useNavigate();
  const unread = NOTIFICATIONS.filter(n => !n.read).length;
  const now = new Date();

  return (
    <header className="header">
      <div>
        <div className="header-title">{title}</div>
        {subtitle && <div className="header-sub">{subtitle}</div>}
      </div>

      <div className="header-right">
        <div style={{ fontSize: 12, color: 'var(--text3)', textAlign: 'right' }}>
          <div style={{ fontWeight: 600, color: 'var(--text2)' }}>
            {now.toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' })}
          </div>
          <div>Bengaluru Municipal Authority</div>
        </div>

        <button className="btn btn-secondary btn-sm" onClick={() => window.location.reload()} title="Refresh">
          <RefreshCw size={14} />
        </button>

        <button className="btn btn-secondary btn-sm" onClick={toggleTheme} title="Toggle theme">
          {isDark ? <Sun size={14} /> : <Moon size={14} />}
        </button>

        <button className="notif-btn" onClick={() => navigate('/notifications')}>
          <Bell size={16} />
          {unread > 0 && <span className="notif-dot" />}
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'var(--card2)', border: '1px solid var(--border)', borderRadius: 8, padding: '6px 12px', cursor: 'pointer' }}>
          <div style={{ width: 28, height: 28, borderRadius: 8, background: 'linear-gradient(135deg, #38bdf8, #3b82f6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, color: '#0f172a' }}>
            {user?.avatar || 'AU'}
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text)' }}>{user?.name?.split(' ')[0]}</div>
            <div style={{ fontSize: 10, color: 'var(--text3)' }}>{user?.role}</div>
          </div>
        </div>
      </div>
    </header>
  );
}
