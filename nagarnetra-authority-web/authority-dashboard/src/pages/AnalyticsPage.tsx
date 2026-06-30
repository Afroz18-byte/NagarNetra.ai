import Header from '../components/Header';
import { TREND_DATA, CATEGORY_DATA, DEPARTMENT_STATS, WARD_STATS } from '../data/mockData';
import { AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell, RadarChart, Radar, PolarGrid, PolarAngleAxis, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Download, FileText } from 'lucide-react';
import { useState } from 'react';

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 14px', fontSize: 12 }}>
      <div style={{ fontWeight: 700, marginBottom: 4 }}>{label}</div>
      {payload.map((p: any) => <div key={p.name} style={{ color: p.color }}>{p.name}: {p.value}</div>)}
    </div>
  );
};

export default function AnalyticsPage() {
  const [dateRange, setDateRange] = useState('6m');

  const radarData = DEPARTMENT_STATS.map(d => ({
    dept: d.name,
    'Resolution Rate': Math.round((d.resolved / d.totalIssues) * 100),
    'Response Speed': Math.round((7 - d.avgResolutionDays) * 14),
    'Issue Volume': Math.round((d.totalIssues / 142) * 100),
  }));

  const wardData = WARD_STATS.map(w => ({
    ward: w.ward.replace('Ward ', 'W'),
    Issues: w.issues,
    Resolved: w.resolved,
    'Pending': w.issues - w.resolved,
  }));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Analytics & Reports" subtitle="Comprehensive civic performance insights" />
      <div className="page-body fade-in">
        {/* Controls */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 20, alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: 6 }}>
            {['1m', '3m', '6m', '1y', 'All'].map(r => (
              <button key={r} className={`btn btn-sm ${dateRange === r ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setDateRange(r)}>{r}</button>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary btn-sm"><Download size={13} /> Export CSV</button>
            <button className="btn btn-primary btn-sm"><FileText size={13} /> Generate PDF Report</button>
          </div>
        </div>

        {/* Summary stats */}
        <div className="grid-4 mb-4">
          {[
            { l: 'Total Reports', v: '390', c: '#38bdf8', delta: '+67 this month' },
            { l: 'Resolution Rate', v: '73%', c: '#10b981', delta: '+5% vs last month' },
            { l: 'Avg Resolution Time', v: '4.1d', c: '#8b5cf6', delta: '-1.1d improvement' },
            { l: 'Citizen Satisfaction', v: '4.2/5', c: '#f59e0b', delta: '+0.3 this month' },
          ].map(({ l, v, c, delta }) => (
            <div key={l} className="stat-card" style={{ '--accent-color': c } as React.CSSProperties}>
              <div className="label">{l}</div>
              <div className="value" style={{ color: c }}>{v}</div>
              <div className="trend" style={{ color: c, fontSize: 11 }}>▲ {delta}</div>
            </div>
          ))}
        </div>

        {/* Trend chart */}
        <div className="card mb-4">
          <div className="card-header"><span className="card-title">📈 Issue Trends Over Time</span></div>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={TREND_DATA} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
              <XAxis dataKey="month" tick={{ fontSize: 12, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Area type="monotone" dataKey="reported" stroke="#38bdf8" fill="rgba(56,189,248,0.1)" strokeWidth={2} name="Reported" />
              <Area type="monotone" dataKey="resolved" stroke="#10b981" fill="rgba(16,185,129,0.1)" strokeWidth={2} name="Resolved" />
              <Area type="monotone" dataKey="escalated" stroke="#ef4444" fill="rgba(239,68,68,0.1)" strokeWidth={2} name="Escalated" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="grid-2 mb-4">
          {/* Category distribution */}
          <div className="card">
            <div className="card-title mb-3">🏷️ Category Distribution</div>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <ResponsiveContainer width="50%" height={180}>
                <PieChart>
                  <Pie data={CATEGORY_DATA} cx="50%" cy="50%" innerRadius={45} outerRadius={70} dataKey="value" paddingAngle={2}>
                    {CATEGORY_DATA.map((c, i) => <Cell key={i} fill={c.color} />)}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ flex: 1 }}>
                {CATEGORY_DATA.map(c => (
                  <div key={c.name} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '5px 0', borderBottom: '1px solid var(--border)', fontSize: 12 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <div style={{ width: 8, height: 8, borderRadius: '50%', background: c.color }} />
                      <span style={{ color: 'var(--text2)' }}>{c.name}</span>
                    </div>
                    <span style={{ fontWeight: 700 }}>{c.value}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Dept radar */}
          <div className="card">
            <div className="card-title mb-3">🏛️ Department Performance Radar</div>
            <ResponsiveContainer width="100%" height={200}>
              <RadarChart data={radarData}>
                <PolarGrid stroke="var(--border)" />
                <PolarAngleAxis dataKey="dept" tick={{ fontSize: 11, fill: 'var(--text2)' }} />
                <Radar name="Resolution Rate" dataKey="Resolution Rate" stroke="#10b981" fill="rgba(16,185,129,0.15)" strokeWidth={2} />
                <Radar name="Response Speed" dataKey="Response Speed" stroke="#38bdf8" fill="rgba(56,189,248,0.1)" strokeWidth={2} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Tooltip content={<CustomTooltip />} />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Ward comparison */}
        <div className="card mb-4">
          <div className="card-title mb-3">🗺️ Ward-wise Performance</div>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={wardData} barCategoryGap="25%">
              <XAxis dataKey="ward" tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: 'var(--text3)' }} axisLine={false} tickLine={false} />
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Bar dataKey="Resolved" fill="#10b981" radius={[4, 4, 0, 0]} stackId="a" />
              <Bar dataKey="Pending" fill="#f59e0b" radius={[4, 4, 0, 0]} stackId="a" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* PredictBot zone alerts */}
        <div className="card">
          <div className="card-title mb-3">🔮 PredictBot — High Risk Zones</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12 }}>
            {[
              { zone: 'Whitefield', risk: 'Pothole Cluster', confidence: 87, level: 'high' },
              { zone: 'Hebbal', risk: 'Waterlogging', confidence: 82, level: 'high' },
              { zone: 'Koramangala', risk: 'Lights Failure', confidence: 74, level: 'medium' },
              { zone: 'Bellandur', risk: 'Garbage Overflow', confidence: 69, level: 'medium' },
              { zone: 'Yelahanka', risk: 'Water Contamination', confidence: 91, level: 'critical' },
              { zone: 'MG Road', risk: 'Manhole Hazards', confidence: 78, level: 'high' },
            ].map(({ zone, risk, confidence, level }) => (
              <div key={zone} style={{ background: level === 'critical' ? 'rgba(239,68,68,0.07)' : level === 'high' ? 'rgba(245,158,11,0.07)' : 'rgba(59,130,246,0.07)', border: `1px solid ${level === 'critical' ? 'rgba(239,68,68,0.2)' : level === 'high' ? 'rgba(245,158,11,0.2)' : 'rgba(59,130,246,0.2)'}`, borderRadius: 10, padding: '12px 14px' }}>
                <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 4 }}>{zone}</div>
                <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 8 }}>{risk}</div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span className={`badge badge-${level}`}>{level}</span>
                  <span style={{ fontSize: 12, fontWeight: 700 }}>{confidence}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
