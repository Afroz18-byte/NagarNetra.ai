import { useNavigate } from 'react-router-dom';
import { AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { TREND_DATA, NOTIFICATIONS } from '../data/mockData';
import { AlertTriangle, CheckCircle2, Clock, TrendingUp, FileText, Zap, Map } from 'lucide-react';

function StatCard({ label, value, sub, color, icon: Icon }: { label: string; value: string | number; sub?: string; color: string; icon: React.ElementType }) {
  return (
    <div className="stat-card" style={{ '--accent-color': color, '--icon-bg': `${color}20` } as React.CSSProperties}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div className="label">{label}</div>
          <div className="value" style={{ color }}>{value}</div>
          {sub && <div className="trend text-muted">{sub}</div>}
        </div>
        <div className="icon-wrap">
          <Icon size={22} color={color} />
        </div>
      </div>
    </div>
  );
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 14px', fontSize: 12 }}>
      <div style={{ fontWeight: 700, marginBottom: 6 }}>{label}</div>
      {payload.map((p: any) => <div key={p.name} style={{ color: p.color }}>{p.name}: {p.value}</div>)}
    </div>
  );
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const { issues, logs, agents } = useData();

  const total = issues.length;
  const resolved = issues.filter(i => i.status === 'Resolved').length;
  const escalated = issues.filter(i => i.status === 'Escalated' || i.slaStatus === 'SLA BREACHED').length;
  const open = issues.filter(i => i.status === 'Open').length;
  const inProgress = issues.filter(i => i.status === 'In Progress').length;

  const now = Date.now();
  const dayMs = 24 * 60 * 60 * 1000;
  const weekMs = 7 * dayMs;
  const totalToday = issues.filter(i => now - new Date(i.timePosted).getTime() < dayMs).length || 3;
  const totalWeek = issues.filter(i => now - new Date(i.timePosted).getTime() < weekMs).length || issues.length;

  const topIssues = [...issues]
    .map(i => {
      // Base score
      const baseSev = i.severity * 8;
      const baseUp = Math.min(20, i.upvotes * 2);
      const priorityScore = Math.min(100, baseSev + baseUp);
      return { ...i, priorityScore };
    })
    .sort((a, b) => b.priorityScore - a.priorityScore)
    .slice(0, 5);

  const statusData = [
    { name: 'Open', value: open, color: '#3b82f6' },
    { name: 'In Progress', value: inProgress, color: '#f59e0b' },
    { name: 'Resolved', value: resolved, color: '#10b981' },
    { name: 'Escalated', value: escalated, color: '#ef4444' },
  ];

  // Dynamic Department Stats
  const departments: Record<string, { total: number; resolved: number; pending: number }> = {
    PWD: { total: 0, resolved: 0, pending: 0 },
    BWSSB: { total: 0, resolved: 0, pending: 0 },
    BESCOM: { total: 0, resolved: 0, pending: 0 },
    BBMP: { total: 0, resolved: 0, pending: 0 },
  };

  issues.forEach(i => {
    const dept = i.assignedDept || 'BBMP';
    if (departments[dept]) {
      departments[dept].total += 1;
      if (i.status === 'Resolved') {
        departments[dept].resolved += 1;
      } else {
        departments[dept].pending += 1;
      }
    }
  });

  const departmentStats = Object.keys(departments).map(name => ({
    name,
    resolved: departments[name].resolved,
    pending: departments[name].pending,
  }));

  // Dynamic Category Stats
  const categories: Record<string, { count: number; color: string }> = {
    Pothole: { count: 0, color: '#8B5CF6' },
    Water: { count: 0, color: '#3B82F6' },
    Lights: { count: 0, color: '#F59E0B' },
    Garbage: { count: 0, color: '#F97316' },
    Sewage: { count: 0, color: '#B45309' },
    Pedestrian: { count: 0, color: '#10B981' },
    Other: { count: 0, color: '#EF4444' },
  };

  issues.forEach(i => {
    const cat = i.category || 'Other';
    if (categories[cat]) {
      categories[cat].count += 1;
    } else {
      categories['Other'].count += 1;
    }
  });

  const categoryData = Object.keys(categories).map(name => ({
    name,
    value: categories[name].count,
    color: categories[name].color,
  }));

  // Dynamic Recent Activity Logs
  const recentLogs = logs.length > 0 ? logs.slice(0, 5).map((log, index) => {
    const timeStr = new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    return {
      id: `log-${index}`,
      type: log.priority === 'Critical' ? 'escalation' : log.priority === 'High' ? 'high_priority' : 'new_report',
      title: log.isBotAction ? '🤖 Bot Event' : '👤 Dispatcher Event',
      message: log.message,
      time: timeStr,
      read: false
    };
  }) : NOTIFICATIONS.slice(0, 5);

  const getBadge = (status: string) => {
    const map: Record<string, string> = { 'Open': 'badge-open', 'Reported': 'badge-open', 'In Progress': 'badge-progress', 'Resolved': 'badge-resolved', 'Escalated': 'badge-escalated' };
    return map[status] || 'badge-open';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Authority Dashboard" subtitle="Bengaluru Civic Issue Management — Live Overview" />
      <div className="page-body fade-in">
        {/* Alert bar */}
        {escalated > 0 && (
          <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 10, padding: '12px 16px', marginBottom: 20, display: 'flex', alignItems: 'center', gap: 10 }}>
            <AlertTriangle size={16} color="var(--red)" />
            <span style={{ fontSize: 13, color: 'var(--red)', fontWeight: 600 }}>{escalated} issues escalated — immediate action required</span>
            <button className="btn btn-danger btn-sm" style={{ marginLeft: 'auto' }} onClick={() => navigate('/escalations')}>View Escalations</button>
          </div>
        )}

        {/* Stat Cards */}
        <div className="grid-4 mb-4">
          <StatCard label="Today's Reports" value={totalToday} sub="+3 from yesterday" color="#38bdf8" icon={FileText} />
          <StatCard label="This Week" value={totalWeek} sub="Synchronized from mobile app" color="#3b82f6" icon={TrendingUp} />
          <StatCard label="Resolved" value={resolved} sub={`${total > 0 ? Math.round((resolved / total) * 100) : 0}% resolution rate`} color="#10b981" icon={CheckCircle2} />
          <StatCard label="Escalated" value={escalated} sub="Require immediate action" color="#ef4444" icon={AlertTriangle} />
        </div>
        <div className="grid-4 mb-4">
          <StatCard label="Open Issues" value={open} sub="Awaiting assignment" color="#f59e0b" icon={Clock} />
          <StatCard label="Avg Resolution" value="3.8d" sub="Down from 5.2 days" color="#8b5cf6" icon={Zap} />
          <StatCard label="Active Zones" value="12" sub="Wards with activity" color="#06b6d4" icon={Map} />
          <StatCard label="Duplicate Merges" value="4" sub="AI-detected this cycle" color="#ec4899" icon={CheckCircle2} />
        </div>

        {/* Charts row */}
        <div className="grid-2 mb-4">
          <div className="card">
            <div className="card-header">
              <span className="card-title">📈 Issue Trends (6 Months)</span>
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/analytics')}>Full Analytics →</button>
            </div>
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={TREND_DATA}>
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Area type="monotone" dataKey="reported" stroke="#38bdf8" fill="rgba(56,189,248,0.1)" strokeWidth={2} name="Reported" />
                <Area type="monotone" dataKey="resolved" stroke="#10b981" fill="rgba(16,185,129,0.1)" strokeWidth={2} name="Resolved" />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <div className="card-header">
              <span className="card-title">🏛️ Department Performance</span>
            </div>
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={departmentStats} layout="vertical">
                <XAxis type="number" tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} width={50} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="resolved" fill="#10b981" name="Resolved" radius={[0, 4, 4, 0]} />
                <Bar dataKey="pending" fill="#f59e0b" name="Pending" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status breakdown + Category pie */}
        <div className="grid-2 mb-4">
          <div className="card">
            <div className="card-header"><span className="card-title">📊 Status Breakdown</span></div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
              <ResponsiveContainer width="50%" height={150}>
                <PieChart>
                  <Pie data={statusData} cx="50%" cy="50%" innerRadius={40} outerRadius={65} dataKey="value" paddingAngle={3}>
                    {statusData.map((s, i) => <Cell key={i} fill={s.color} />)}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ flex: 1 }}>
                {statusData.map(s => (
                  <div key={s.name} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div style={{ width: 8, height: 8, borderRadius: '50%', background: s.color }} />
                      <span style={{ fontSize: 12, color: 'var(--text2)' }}>{s.name}</span>
                    </div>
                    <span style={{ fontWeight: 700, color: s.color }}>{s.value}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header"><span className="card-title">🏷️ Issue Categories</span></div>
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={categoryData}>
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="value" name="Issues" radius={[4, 4, 0, 0]}>
                  {categoryData.map((c, i) => <Cell key={i} fill={c.color} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Top priority + activity */}
        <div className="grid-2 mb-4">
          <div className="card">
            <div className="card-header">
              <span className="card-title">🚨 Top 5 Priority Issues</span>
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/reports')}>View All →</button>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {topIssues.map((issue, i) => (
                <div key={issue.id} onClick={() => navigate(`/reports/${issue.id}`)} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 12px', background: 'var(--card2)', borderRadius: 8, cursor: 'pointer', border: '1px solid var(--border)', transition: 'border-color 0.2s' }}
                  onMouseEnter={e => (e.currentTarget.style.borderColor = 'var(--accent)')}
                  onMouseLeave={e => (e.currentTarget.style.borderColor = 'var(--border)')}>
                  <div style={{ width: 24, height: 24, borderRadius: 6, background: i < 2 ? 'rgba(239,68,68,0.2)' : 'rgba(245,158,11,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, color: i < 2 ? 'var(--red)' : 'var(--yellow)', flexShrink: 0 }}>#{i + 1}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{issue.title}</div>
                    <div style={{ fontSize: 11, color: 'var(--text3)' }}>{issue.locationName} · {issue.assignedDept}</div>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                    <span style={{ fontSize: 13, fontWeight: 800, color: issue.priorityScore >= 90 ? 'var(--red)' : issue.priorityScore >= 70 ? 'var(--yellow)' : 'var(--green)' }}>{issue.priorityScore}</span>
                    <span className={`badge ${getBadge(issue.status)}`}>{issue.status}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <span className="card-title">⚡ Recent Activity</span>
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/notifications')}>All →</button>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              {recentLogs.map(n => (
                <div key={n.id} style={{ padding: '10px 0', borderBottom: '1px solid var(--border)', display: 'flex', gap: 10 }}>
                  <div style={{ width: 8, height: 8, borderRadius: '50%', marginTop: 5, flexShrink: 0, background: n.type === 'escalation' ? 'var(--red)' : n.type === 'resolved' ? 'var(--green)' : n.type === 'high_priority' ? 'var(--yellow)' : 'var(--accent)' }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text)' }}>{n.title}</div>
                    <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 2, lineBreak: 'anywhere' }}>{n.message}</div>
                    <div style={{ fontSize: 10, color: 'var(--text3)', marginTop: 3 }}>{n.time}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* AI Agents quick status */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">🤖 AI Agent Status</span>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/agents')}>Full Panel →</button>
          </div>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            {agents.map(a => (
              <div key={a.id} style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'var(--card2)', border: '1px solid var(--border)', borderRadius: 8, padding: '8px 12px', flex: '1 1 200px' }}>
                <span style={{ fontSize: 18 }}>{a.icon}</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12, fontWeight: 600 }}>{a.name}</div>
                  <div style={{ fontSize: 11, color: 'var(--text3)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.lastAction}</div>
                </div>
                <span className={`status-dot dot-${a.status.toLowerCase()}`} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
