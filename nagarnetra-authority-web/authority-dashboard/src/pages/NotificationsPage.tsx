import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { NOTIFICATIONS } from '../data/mockData';
import { useState, useMemo } from 'react';
import { Bell, AlertTriangle, CheckCircle2, Clock, FileText, Trash2, CheckCheck } from 'lucide-react';

const typeIcon: Record<string, React.ElementType> = {
  high_priority: AlertTriangle,
  escalation: AlertTriangle,
  overdue: Clock,
  resolved: CheckCircle2,
  new_report: FileText,
};

const typeColor: Record<string, string> = {
  high_priority: '#f59e0b',
  escalation: '#ef4444',
  overdue: '#f59e0b',
  resolved: '#10b981',
  new_report: '#38bdf8',
};

export default function NotificationsPage() {
  const { logs } = useData();
  const [filter, setFilter] = useState<string>('all');
  const [localNotifs, setLocalNotifs] = useState<any[]>([]);

  // Dynamically create notifications from backend logs + mock data
  const notifs = useMemo(() => {
    const backendNotifs = logs.map((log, index) => {
      const type = log.priority === 'Critical' ? 'escalation' : log.priority === 'High' ? 'high_priority' : 'new_report';
      return {
        id: `log-${index}-${log.timestamp}`,
        type,
        title: log.isBotAction ? '🤖 AI Orchestrator Alert' : '👤 System Event',
        message: log.message,
        time: new Date(log.timestamp).toLocaleString('en-IN'),
        read: false
      };
    });

    const combined = [...backendNotifs, ...NOTIFICATIONS];
    // filter out deleted notifs in localNotifs
    return combined.filter(c => !localNotifs.some(ln => ln.id === c.id && ln.deleted))
                   .map(c => {
                     const local = localNotifs.find(ln => ln.id === c.id);
                     if (local) return { ...c, read: local.read };
                     return c;
                   });
  }, [logs, localNotifs]);

  const markAll = () => {
    const updated = notifs.map(n => ({ id: n.id, read: true, deleted: false }));
    setLocalNotifs(updated);
  };

  const markRead = (id: string) => {
    setLocalNotifs(prev => {
      const index = prev.findIndex(x => x.id === id);
      if (index >= 0) {
        const copy = [...prev];
        copy[index] = { ...copy[index], read: true };
        return copy;
      }
      return [...prev, { id, read: true, deleted: false }];
    });
  };

  const deleteNotif = (id: string) => {
    setLocalNotifs(prev => {
      const index = prev.findIndex(x => x.id === id);
      if (index >= 0) {
        const copy = [...prev];
        copy[index] = { ...copy[index], deleted: true };
        return copy;
      }
      return [...prev, { id, read: true, deleted: true }];
    });
  };

  const filtered = notifs.filter(n => filter === 'all' ? true : filter === 'unread' ? !n.read : n.type === filter);
  const unread = notifs.filter(n => !n.read).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Notifications" subtitle={`${unread} unread notifications`} />
      <div className="page-body fade-in">
        {/* Controls */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 10 }}>
          <div style={{ display: 'flex', gap: 6 }}>
            {[
              ['all', 'All', notifs.length],
              ['unread', 'Unread', unread],
              ['escalation', 'Escalations', notifs.filter(n => n.type === 'escalation').length],
              ['high_priority', 'High Priority', notifs.filter(n => n.type === 'high_priority').length],
              ['resolved', 'Resolved', notifs.filter(n => n.type === 'resolved').length],
            ].map(([v, l, c]) => (
              <button key={String(v)} className={`btn btn-sm ${filter === v ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setFilter(String(v))}>
                {l} {Number(c) > 0 && <span style={{ background: 'rgba(255,255,255,0.2)', borderRadius: 10, padding: '0 5px', fontSize: 10 }}>{c}</span>}
              </button>
            ))}
          </div>
          <button className="btn btn-secondary btn-sm" onClick={markAll}><CheckCheck size={13} /> Mark All Read</button>
        </div>

        {/* Notifications list */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {filtered.length === 0 && (
            <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text3)' }}>
              <Bell size={40} style={{ margin: '0 auto 12px', opacity: 0.3, display: 'block' }} />
              <div style={{ fontSize: 14 }}>No notifications to show</div>
            </div>
          )}
          {filtered.map(n => {
            const Icon = typeIcon[n.type] || Bell;
            const color = typeColor[n.type] || '#38bdf8';
            return (
              <div key={n.id} style={{ background: n.read ? 'var(--card)' : 'var(--card2)', border: `1px solid ${n.read ? 'var(--border)' : color + '40'}`, borderRadius: 10, padding: '14px 16px', display: 'flex', gap: 14, alignItems: 'flex-start', transition: 'all 0.2s' }}>
                <div style={{ width: 38, height: 38, borderRadius: 10, background: `${color}15`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                  <Icon size={18} color={color} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ fontSize: 13, fontWeight: n.read ? 500 : 700, color: n.read ? 'var(--text2)' : 'var(--text)' }}>{n.title}</div>
                    {!n.read && <span style={{ width: 8, height: 8, borderRadius: '50%', background: color, flexShrink: 0, marginTop: 4 }} />}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 4, lineHeight: 1.5, lineBreak: 'anywhere' }}>{n.message}</div>
                  <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 6 }}>🕐 {n.time}</div>
                </div>
                <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                  {!n.read && <button className="btn btn-ghost btn-sm" onClick={() => markRead(n.id)} title="Mark Read"><CheckCircle2 size={13} /></button>}
                  <button className="btn btn-ghost btn-sm" onClick={() => deleteNotif(n.id)} title="Delete"><Trash2 size={13} /></button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Notification preferences */}
        <div className="card mt-4">
          <div className="card-title mb-3">⚙️ Notification Preferences</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            {[
              ['High Priority Reports', true],
              ['Escalation Alerts', true],
              ['SLA Breach Warnings', true],
              ['Issue Resolved Updates', false],
              ['Monthly Report Ready', true],
              ['New Reports in Flagged Zones', true],
              ['Email Notifications', true],
              ['In-App Notifications', true],
            ].map(([l, v]) => (
              <div key={String(l)} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: 'var(--card2)', borderRadius: 8, fontSize: 13 }}>
                <span style={{ color: 'var(--text2)' }}>{l}</span>
                <div style={{ width: 36, height: 20, borderRadius: 10, background: v ? 'var(--green)' : 'var(--border)', position: 'relative', cursor: 'pointer', flexShrink: 0 }}>
                  <div style={{ position: 'absolute', top: 3, left: v ? 18 : 3, width: 14, height: 14, borderRadius: '50%', background: '#fff', transition: 'left 0.2s' }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
