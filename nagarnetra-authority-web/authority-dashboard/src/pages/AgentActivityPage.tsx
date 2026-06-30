import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { useState } from 'react';
import { RefreshCw } from 'lucide-react';

export default function AgentActivityPage() {
  const { agents, refreshData } = useData();
  const [selected, setSelected] = useState<string | null>(null);


  const totalToday = agents.reduce((s, a) => s + a.actionsToday, 0);
  const active = agents.filter(a => a.status === 'Active').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="AI Agent Activity" subtitle="Real-time view of all 8 NagarNetra AI agents" />
      <div className="page-body fade-in">
        {/* Summary */}
        <div className="grid-4 mb-4">
          {[
            { l: 'Active Agents', v: active, c: '#10b981' },
            { l: 'Actions Today', v: totalToday, c: '#38bdf8' },
            { l: 'Total Actions', v: agents.reduce((s, a) => s + a.totalActions, 0).toLocaleString(), c: '#8b5cf6' },
            { l: 'Idle Agents', v: agents.filter(a => a.status === 'Idle').length, c: '#64748b' },
          ].map(({ l, v, c }) => (
            <div key={l} className="stat-card" style={{ '--accent-color': c } as React.CSSProperties}>
              <div className="label">{l}</div>
              <div className="value" style={{ color: c }}>{v}</div>
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12, gap: 8 }}>
          <button className="btn btn-secondary btn-sm" onClick={refreshData}><RefreshCw size={13} /> Refresh Now</button>
        </div>

        {/* Agent grid */}
        <div className="grid-2 mb-4">
          {agents.map(agent => (
            <div key={agent.id} className="agent-card" style={{ cursor: 'pointer', border: `1px solid ${selected === agent.id ? agent.color : 'var(--border)'}`, transition: 'border-color 0.2s' }} onClick={() => setSelected(selected === agent.id ? null : agent.id)}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div className="agent-icon" style={{ background: `${agent.color}20` }}>{agent.icon}</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span className={`status-dot dot-${agent.status.toLowerCase()}`} />
                  <span style={{ fontSize: 11, color: agent.status === 'Active' ? 'var(--green)' : agent.status === 'Processing' ? 'var(--yellow)' : 'var(--text3)', fontWeight: 600 }}>{agent.status}</span>
                </div>
              </div>

              <div style={{ fontSize: 15, fontWeight: 800, color: 'var(--text)' }}>{agent.name}</div>
              <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 12 }}>{agent.role}</div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 12 }}>
                <div style={{ background: 'var(--card2)', borderRadius: 8, padding: '8px 10px' }}>
                  <div style={{ fontSize: 18, fontWeight: 800, color: agent.color }}>{agent.actionsToday}</div>
                  <div style={{ fontSize: 10, color: 'var(--text3)' }}>Today</div>
                </div>
                <div style={{ background: 'var(--card2)', borderRadius: 8, padding: '8px 10px' }}>
                  <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--text)' }}>{agent.totalActions.toLocaleString()}</div>
                  <div style={{ fontSize: 10, color: 'var(--text3)' }}>Total</div>
                </div>
              </div>

              <div style={{ fontSize: 11, color: 'var(--text2)', borderLeft: `2px solid ${agent.color}`, paddingLeft: 8, marginBottom: 10 }}>
                <span style={{ color: 'var(--text3)' }}>Last: </span>{agent.lastAction.slice(0, 70)}...
              </div>
              <div style={{ fontSize: 10, color: 'var(--text3)' }}>⏰ {agent.lastActionTime}</div>

              {selected === agent.id && (
                <div className="agent-logs fade-in">
                  <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text2)', marginBottom: 6 }}>Recent Actions</div>
                  {agent.recentLogs.map((log, i) => (
                    <div key={i} className="log-item" style={{ lineBreak: 'anywhere' }}>{log}</div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Orchestration flow */}
        <div className="card">
          <div className="card-title mb-3">🔄 Agent Pipeline Flow</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 0, overflowX: 'auto', padding: '8px 0' }}>
            {['VisionBot', 'CategoryBot', 'DuplicateBot', 'PriorityBot', 'PredictBot', 'EscalationBot', 'ReportBot'].map((name, i) => {
              const agent = agents.find(a => a.name === name)!;
              return (
                <div key={name} style={{ display: 'flex', alignItems: 'center' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, minWidth: 100, padding: '12px 8px', background: 'var(--card2)', borderRadius: 10, border: `1px solid ${agent?.color || 'var(--border)'}30` }}>
                    <span style={{ fontSize: 22 }}>{agent?.icon || '🤖'}</span>
                    <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--text)' }}>{name.replace('Bot', '')}</span>
                    <span className={`status-dot dot-${agent?.status?.toLowerCase() || 'idle'}`} />
                  </div>
                  {i < 6 && <div style={{ width: 32, height: 2, background: 'var(--border)', position: 'relative', flexShrink: 0 }}>
                    <div style={{ position: 'absolute', right: -5, top: -4, fontSize: 10, color: 'var(--text3)' }}>›</div>
                  </div>}
                </div>
              );
            })}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 10, textAlign: 'center' }}>
            OrchestratorBot coordinates all agents → Routes citizen reports through the full AI pipeline automatically
          </div>
        </div>
      </div>
    </div>
  );
}
