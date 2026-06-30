import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { DEPARTMENT_STATS } from '../data/mockData';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { Plus } from 'lucide-react';
import { useState, useMemo } from 'react';

const deptIcons: Record<string, string> = { BBMP: '🏛️', PWD: '🚧', BWSSB: '💧', BESCOM: '⚡' };

export default function DepartmentsPage() {
  const { issues } = useData();
  const [selected, setSelected] = useState<string | null>(null);

  // Dynamically compute department stats based on synchronized issues
  const computedDeptStats = useMemo(() => {
    return DEPARTMENT_STATS.map(dept => {
      const deptIssues = issues.filter(i => (i.assignedDept || 'BBMP') === dept.name);
      const totalIssues = deptIssues.length;
      const resolved = deptIssues.filter(i => i.status === 'Resolved').length;
      const escalated = deptIssues.filter(i => i.status === 'Escalated' || i.slaStatus === 'SLA BREACHED').length;
      const pending = totalIssues - resolved - escalated;

      const resolvedIssues = deptIssues.filter(i => i.status === 'Resolved');
      const avgDays = resolvedIssues.length > 0
        ? Math.round(resolvedIssues.reduce((sum, i) => {
            const posted = new Date(i.timePosted).getTime();
            const days = Math.max(1, Math.round((Date.now() - posted) / (1000 * 60 * 60 * 24)));
            return sum + days;
          }, 0) / resolvedIssues.length)
        : dept.avgResolutionDays;

      return {
        ...dept,
        totalIssues: Math.max(1, totalIssues),
        resolved,
        pending,
        escalated,
        avgResolutionDays: avgDays
      };
    });
  }, [issues]);

  const sel = computedDeptStats.find(d => d.name === selected);
  
  const selIssues = useMemo(() => {
    if (!selected) return [];
    return issues.filter(i => (i.assignedDept || 'BBMP') === selected).map(i => {
      const severity = i.severity || 5;
      const upvotes = i.upvotes || 0;
      const priorityScore = Math.min(100, severity * 8 + Math.min(20, upvotes * 2));
      
      const postedTime = new Date(i.timePosted).getTime();
      const daysOpen = Math.max(1, Math.round((Date.now() - postedTime) / (1000 * 60 * 60 * 24)));

      return {
        ...i,
        priorityScore,
        daysOpen
      };
    });
  }, [issues, selected]);

  const chartData = computedDeptStats.map(d => ({
    name: d.name,
    Resolved: d.resolved,
    Pending: d.pending,
    Escalated: d.escalated,
    color: d.color,
  }));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Departments" subtitle="Municipal department performance and management" />
      <div className="page-body fade-in">
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
          <button className="btn btn-primary"><Plus size={14} /> Add Department</button>
        </div>

        <div className="grid-2 mb-4">
          {computedDeptStats.map(dept => {
            const resRate = Math.round((dept.resolved / dept.totalIssues) * 100) || 0;
            return (
              <div key={dept.name} className="card" style={{ cursor: 'pointer', borderColor: selected === dept.name ? dept.color : 'var(--border)', transition: 'border-color 0.2s' }} onClick={() => setSelected(selected === dept.name ? null : dept.name)}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ width: 48, height: 48, borderRadius: 14, background: `${dept.color}20`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22 }}>
                      {deptIcons[dept.name]}
                    </div>
                    <div>
                      <div style={{ fontSize: 16, fontWeight: 800 }}>{dept.name}</div>
                      <div style={{ fontSize: 11, color: 'var(--text3)', maxWidth: 160 }}>{dept.displayName}</div>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 24, fontWeight: 800, color: dept.color }}>{resRate}%</div>
                    <div style={{ fontSize: 10, color: 'var(--text3)' }}>Resolution Rate</div>
                  </div>
                </div>

                {/* Resolution progress */}
                <div style={{ height: 6, background: 'var(--bg)', borderRadius: 3, overflow: 'hidden', marginBottom: 12 }}>
                  <div style={{ width: `${resRate}%`, height: '100%', background: dept.color, borderRadius: 3 }} />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
                  {[
                    { l: 'Total', v: dept.totalIssues, c: 'var(--text)' },
                    { l: 'Resolved', v: dept.resolved, c: '#10b981' },
                    { l: 'Pending', v: dept.pending, c: '#f59e0b' },
                    { l: 'Escalated', v: dept.escalated, c: '#ef4444' },
                  ].map(({ l, v, c }) => (
                    <div key={l} style={{ textAlign: 'center', background: 'var(--card2)', borderRadius: 8, padding: '8px 4px' }}>
                      <div style={{ fontSize: 18, fontWeight: 800, color: c }}>{v}</div>
                      <div style={{ fontSize: 10, color: 'var(--text3)' }}>{l}</div>
                    </div>
                  ))}
                </div>

                <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
                  <span style={{ color: 'var(--text3)' }}>Head: <span style={{ color: 'var(--text2)', fontWeight: 600 }}>{dept.head}</span></span>
                  <span style={{ color: 'var(--text3)' }}>Avg: <span style={{ color: dept.color, fontWeight: 700 }}>{dept.avgResolutionDays}d</span></span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Performance chart */}
        <div className="card mb-4">
          <div className="card-title mb-3">📊 Department Comparison</div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={chartData} barCategoryGap="30%">
              <XAxis dataKey="name" tick={{ fontSize: 12, fill: 'var(--text2)' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, fontSize: 12 }} />
              <Bar dataKey="Resolved" fill="#10b981" radius={[4, 4, 0, 0]} />
              <Bar dataKey="Pending" fill="#f59e0b" radius={[4, 4, 0, 0]} />
              <Bar dataKey="Escalated" fill="#ef4444" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Selected dept issues */}
        {sel && (
          <div className="card fade-in">
            <div className="card-header">
              <span className="card-title">{deptIcons[sel.name]} {sel.name} — Active Issues</span>
              <button className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>✕ Close</button>
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th><th>Title</th><th>Status</th><th>Priority</th><th>Location</th><th>Days Open</th>
                  </tr>
                </thead>
                <tbody>
                  {selIssues.length === 0 ? (
                    <tr>
                      <td colSpan={6} style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text3)', fontSize: 13 }}>
                        No active issues in this department.
                      </td>
                    </tr>
                  ) : selIssues.map(i => (
                    <tr key={i.id}>
                      <td style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 11, color: 'var(--accent)' }}>{i.id}</td>
                      <td style={{ fontSize: 12, fontWeight: 600 }}>{i.title}</td>
                      <td><span className={`badge badge-${i.status === 'Open' ? 'open' : i.status === 'In Progress' ? 'progress' : i.status === 'Resolved' ? 'resolved' : 'escalated'}`}>{i.status}</span></td>
                      <td style={{ fontWeight: 700, color: i.priorityScore >= 80 ? 'var(--red)' : 'var(--yellow)' }}>{i.priorityScore}</td>
                      <td style={{ fontSize: 12, color: 'var(--text2)' }}>{i.locationName}</td>
                      <td style={{ fontWeight: 700, color: i.daysOpen > 6 ? 'var(--red)' : 'var(--text)' }}>{i.daysOpen}d</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
