import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Mail, UserCog, Flame, ExternalLink } from 'lucide-react';
import { useState, useMemo } from 'react';

export default function EscalationPage() {
  const navigate = useNavigate();
  const { issues, updateIssueOnServer } = useData();
  const [emailSent, setEmailSent] = useState<string[]>([]);

  const sendEmail = (id: string) => {
    setEmailSent(prev => [...prev, id]);
    alert(`📧 Escalation email sent to department head for ${id}`);
  };

  const computedIssues = useMemo(() => {
    return issues.map(i => {
      const severity = i.severity || 5;
      const upvotes = i.upvotes || 0;
      const priorityScore = Math.min(100, severity * 8 + Math.min(20, upvotes * 2));
      
      const postedTime = new Date(i.timePosted).getTime();
      const daysOpen = Math.max(1, Math.round((Date.now() - postedTime) / (1000 * 60 * 60 * 24)));
      const riskLevel = severity >= 8 ? 'Critical' : severity >= 6 ? 'High' : severity >= 4 ? 'Medium' : 'Low';
      
      return {
        ...i,
        priorityScore,
        daysOpen,
        riskLevel
      };
    });
  }, [issues]);

  const escalated = computedIssues.filter(i => i.status === 'Escalated' || i.slaStatus === 'SLA BREACHED');
  const atRisk = computedIssues.filter(i => (i.slaStatus === 'At Risk' || i.daysOpen >= 3) && i.status !== 'Escalated' && i.status !== 'Resolved' && i.slaStatus !== 'SLA BREACHED');

  const handleEscalate = async (id: string) => {
    await updateIssueOnServer(id, {
      status: 'Escalated',
      slaStatus: 'SLA BREACHED',
      escalationReason: 'Manually escalated by supervisor via Escalation Center'
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Escalation Center" subtitle="Issues requiring immediate authority intervention" />
      <div className="page-body fade-in">
        {/* Summary bar */}
        <div className="grid-4 mb-4">
          <div className="stat-card" style={{ '--accent-color': '#ef4444' } as React.CSSProperties}>
            <div className="label">Currently Escalated</div>
            <div className="value" style={{ color: 'var(--red)' }}>{escalated.length}</div>
          </div>
          <div className="stat-card" style={{ '--accent-color': '#f59e0b' } as React.CSSProperties}>
            <div className="label">At Risk (SLA)</div>
            <div className="value" style={{ color: 'var(--yellow)' }}>{atRisk.length}</div>
          </div>
          <div className="stat-card" style={{ '--accent-color': '#8b5cf6' } as React.CSSProperties}>
            <div className="label">Emails Sent Today</div>
            <div className="value" style={{ color: 'var(--purple)' }}>{emailSent.length}</div>
          </div>
          <div className="stat-card" style={{ '--accent-color': '#10b981' } as React.CSSProperties}>
            <div className="label">Resolved This Week</div>
            <div className="value" style={{ color: 'var(--green)' }}>
              {computedIssues.filter(i => i.status === 'Resolved').length}
            </div>
          </div>
        </div>

        {/* Active escalations */}
        <div className="card mb-4">
          <div className="card-header">
            <span className="card-title"><AlertTriangle size={16} color="var(--red)" /> Active Escalations</span>
            <span className="badge badge-escalated">{escalated.length} Issues</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {escalated.length === 0 ? (
              <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text3)', fontSize: 13 }}>
                🎉 No active escalated tickets!
              </div>
            ) : escalated.map(issue => (
              <div key={issue.id} style={{ background: 'rgba(239,68,68,0.05)', border: '1px solid rgba(239,68,68,0.25)', borderRadius: 10, padding: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <Flame size={14} color="var(--red)" />
                      <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 12, color: 'var(--accent)', fontWeight: 700 }}>{issue.id}</span>
                      <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text)' }}>{issue.title}</span>
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--red)', fontWeight: 600, marginBottom: 4 }}>
                      ⚠️ {issue.escalationReason || 'SLA Timeline Breached'}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text3)' }}>
                      📍 {issue.locationName} · {issue.ward} · Dept: {issue.assignedDept}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--red)' }}>{issue.daysOpen}</div>
                    <div style={{ fontSize: 10, color: 'var(--text3)' }}>Days Open</div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                  <span style={{ fontSize: 11, color: 'var(--text3)' }}>▲ {issue.upvotes} upvotes</span>
                  <span style={{ fontSize: 11, color: 'var(--text3)' }}>Priority: <span style={{ color: 'var(--red)', fontWeight: 700 }}>{issue.priorityScore}</span></span>
                  <span className={`badge badge-${issue.riskLevel.toLowerCase()}`}>{issue.riskLevel}</span>
                  <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
                    <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/reports/${issue.id}`)}>
                      <ExternalLink size={12} /> View
                    </button>
                    <button className="btn btn-secondary btn-sm">
                      <UserCog size={12} /> Reassign
                    </button>
                    <button
                      className={`btn btn-sm ${emailSent.includes(issue.id) ? 'btn-secondary' : 'btn-danger'}`}
                      onClick={() => sendEmail(issue.id)}
                      disabled={emailSent.includes(issue.id)}
                    >
                      <Mail size={12} /> {emailSent.includes(issue.id) ? 'Email Sent ✓' : 'Send Email'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* At-risk issues */}
        <div className="card">
          <div className="card-header">
            <span className="card-title" style={{ color: 'var(--yellow)' }}>⚠️ At-Risk Issues (SLA Warning)</span>
            <span className="badge badge-high">{atRisk.length} Issues</span>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>Title</th><th>Dept</th><th>Priority</th><th>Days Open</th><th>Upvotes</th><th>SLA</th><th>Action</th>
                </tr>
              </thead>
              <tbody>
                {atRisk.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text3)', fontSize: 13 }}>
                      No issues currently at warning state.
                    </td>
                  </tr>
                ) : atRisk.map(i => (
                  <tr key={i.id} onClick={() => navigate(`/reports/${i.id}`)}>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 11, color: 'var(--accent)' }}>{i.id}</td>
                    <td style={{ fontSize: 12, fontWeight: 600 }}>{i.title}</td>
                    <td><span style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent)' }}>{i.assignedDept}</span></td>
                    <td style={{ fontWeight: 700, color: 'var(--yellow)' }}>{i.priorityScore}</td>
                    <td style={{ fontWeight: 700, color: i.daysOpen > 5 ? 'var(--red)' : 'var(--yellow)' }}>{i.daysOpen}d</td>
                    <td style={{ fontWeight: 700 }}>▲ {i.upvotes}</td>
                    <td><span className="badge badge-high">{i.slaStatus || 'At Risk'}</span></td>
                    <td onClick={e => e.stopPropagation()}>
                      <button className="btn btn-danger btn-sm" onClick={() => handleEscalate(i.id)}>
                        <AlertTriangle size={12} /> Escalate
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
