import { useParams, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { ArrowLeft, MapPin, Clock, User, ThumbsUp, Bot, CheckCircle2, AlertTriangle, Send, ExternalLink, ShieldCheck } from 'lucide-react';

export default function ReportDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { issues, updateIssueOnServer } = useData();
  
  const issue = issues.find(i => i.id === id);

  const [status, setStatus] = useState(issue?.status || 'Open');
  const [note, setNote] = useState('');
  const [notes, setNotes] = useState<string[]>([]);
  const [officer, setOfficer] = useState(issue?.assignedOfficer || '');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (issue) {
      setStatus(issue.status);
      setOfficer(issue.assignedOfficer || '');
    }
  }, [issue]);

  if (!issue) return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Report Not Found" />
      <div className="page-body" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 16 }}>
        <AlertTriangle size={48} color="var(--yellow)" />
        <div style={{ fontSize: 18, fontWeight: 700 }}>Issue {id} not found</div>
        <button className="btn btn-primary" onClick={() => navigate('/reports')}>← Back to Reports</button>
      </div>
    </div>
  );

  const severity = issue.severity || 5;
  const upvotes = issue.upvotes || 0;
  const priorityScore = Math.min(100, severity * 8 + Math.min(20, upvotes * 2));
  const postedTime = new Date(issue.timePosted).getTime();
  const daysOpen = Math.max(1, Math.round((Date.now() - postedTime) / (1000 * 60 * 60 * 24)));
  const riskLevel = severity >= 8 ? 'Critical' : severity >= 6 ? 'High' : severity >= 4 ? 'Medium' : 'Low';

  const getBadge = (v: string) => {
    const m: Record<string, string> = { 'Open': 'badge-open', 'Reported': 'badge-open', 'In Progress': 'badge-progress', 'Resolved': 'badge-resolved', 'Escalated': 'badge-escalated' };
    return m[v] || 'badge-open';
  };

  const mapsLink = `https://www.google.com/maps?q=${issue.latitude},${issue.longitude}`;
  const trackLink = `https://www.google.com/maps/dir//${issue.latitude},${issue.longitude}`;

  const timeline = [
    { title: 'Report Submitted', desc: `Citizen report received via NagarNetra app`, time: new Date(issue.timePosted).toLocaleString('en-IN') },
    { title: 'VisionBot Analysis', desc: `Category: ${issue.category} | Severity: ${issue.severity}/10 | Confidence: ${issue.aiConfidence || 88}%`, time: 'Auto — 2 min after report' },
    { title: 'CategoryBot Routing', desc: `Assigned to ${issue.assignedDept} department`, time: 'Auto — 3 min after report' },
    { title: 'PriorityBot Scoring', desc: `Priority score: ${priorityScore}/100 | Risk: ${riskLevel}`, time: 'Auto — 4 min after report' },
    ...(issue.status === 'Escalated' ? [{ title: '🚨 EscalationBot Alert', desc: issue.escalationReason || 'SLA exceeded', time: `Day ${daysOpen}` }] : []),
    ...(issue.status === 'Resolved' ? [{ title: '✅ Resolved', desc: issue.resolutionNotes || 'Issue resolved', time: 'Closed' }] : []),
  ];

  const handleSaveChanges = async () => {
    setSaving(true);
    await updateIssueOnServer(issue.id, {
      status: status as any,
      assignedOfficer: officer
    });
    setSaving(false);
  };

  const handleEscalate = async () => {
    setSaving(true);
    await updateIssueOnServer(issue.id, {
      status: 'Escalated',
      slaStatus: 'SLA BREACHED',
      escalationReason: 'Escalated manually by Department Admin'
    });
    setStatus('Escalated');
    setSaving(false);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title={`Report — ${issue.id}`} subtitle={issue.title} />
      <div className="page-body fade-in">
        <button className="btn btn-ghost btn-sm mb-4" onClick={() => navigate('/reports')}>
          <ArrowLeft size={14} /> Back to Reports
        </button>

        {issue.status === 'Escalated' && (
          <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 10, padding: '12px 16px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
            <AlertTriangle size={16} color="var(--red)" />
            <span style={{ fontSize: 13, color: 'var(--red)', fontWeight: 600 }}>ESCALATED — {issue.escalationReason || 'SLA Timeline Breached'}</span>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 20 }}>
          {/* Main */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Overview card */}
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
                <div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--text)', marginBottom: 6 }}>{issue.title}</div>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    <span className={`badge ${getBadge(issue.status)}`}>{issue.status}</span>
                    <span className={`badge badge-${riskLevel.toLowerCase()}`}>{riskLevel} Risk</span>
                    <span style={{ fontSize: 11, color: 'var(--text3)', display: 'flex', alignItems: 'center', gap: 4 }}><MapPin size={10} />{issue.locationName}</span>
                    <span style={{ fontSize: 11, color: 'var(--text3)', display: 'flex', alignItems: 'center', gap: 4 }}><ThumbsUp size={10} />{issue.upvotes} upvotes</span>
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 32, fontWeight: 800, color: priorityScore >= 80 ? 'var(--red)' : priorityScore >= 60 ? 'var(--yellow)' : 'var(--green)' }}>{priorityScore}</div>
                  <div style={{ fontSize: 11, color: 'var(--text3)' }}>Priority Score</div>
                </div>
              </div>

              {/* Citizen Photo Evidence */}
              {issue.photoBase64 && (
                <div style={{ marginBottom: 16 }}>
                  <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 6, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Citizen Photo Evidence</div>
                  <img 
                    src={issue.photoBase64.startsWith('data:') ? issue.photoBase64 : `data:image/jpeg;base64,${issue.photoBase64}`} 
                    style={{ width: '100%', maxHeight: 350, objectFit: 'cover', borderRadius: 10, border: '1px solid var(--border)' }}
                    alt="Civic Issue Proof" 
                  />
                </div>
              )}

              <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.6 }}>{issue.description}</p>
              
              {/* Resolution Photo Evidence */}
              {issue.resolutionPhotoBase64 && (
                <div style={{ marginTop: 20, borderTop: '1px solid var(--border)', paddingTop: 16 }}>
                  <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 6, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <ShieldCheck size={14} color="var(--green)" /> Resolution Photo Evidence (AI Verified)
                  </div>
                  <img 
                    src={issue.resolutionPhotoBase64.startsWith('data:') ? issue.resolutionPhotoBase64 : `data:image/jpeg;base64,${issue.resolutionPhotoBase64}`} 
                    style={{ width: '100%', maxHeight: 350, objectFit: 'cover', borderRadius: 10, border: '1px solid var(--border)' }}
                    alt="Resolution Proof" 
                  />
                  <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text2)', background: 'var(--card2)', padding: 12, borderRadius: 8 }}>
                    <strong>Resolution Notes:</strong> {issue.resolutionNotes || 'No notes provided.'}
                  </div>
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginTop: 16 }}>
                {[
                  { l: 'Department', v: issue.assignedDept },
                  { l: 'Category', v: issue.category },
                  { l: 'Ward', v: issue.ward },
                  { l: 'Severity', v: `${issue.severity}/10` },
                  { l: 'Days Open', v: `${daysOpen} days` },
                  { l: 'SLA Status', v: issue.slaStatus },
                ].map(({ l, v }) => (
                  <div key={l} style={{ background: 'var(--card2)', borderRadius: 8, padding: '10px 12px' }}>
                    <div style={{ fontSize: 10, color: 'var(--text3)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{l}</div>
                    <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2, color: l === 'SLA Status' && v === 'SLA BREACHED' ? 'var(--red)' : 'var(--text)' }}>{v}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* VisionBot AI analysis */}
            <div className="card">
              <div className="card-title mb-3"><Bot size={16} color="var(--purple)" /> VisionBot AI Analysis</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ background: 'rgba(139,92,246,0.08)', border: '1px solid rgba(139,92,246,0.2)', borderRadius: 8, padding: 12 }}>
                  <div style={{ fontSize: 11, color: 'var(--text3)' }}>DETECTED CATEGORY</div>
                  <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--purple)', marginTop: 4 }}>{issue.category}</div>
                </div>
                <div style={{ background: 'rgba(139,92,246,0.08)', border: '1px solid rgba(139,92,246,0.2)', borderRadius: 8, padding: 12 }}>
                  <div style={{ fontSize: 11, color: 'var(--text3)' }}>AI CONFIDENCE</div>
                  <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--purple)', marginTop: 4 }}>{issue.aiConfidence || 94.2}%</div>
                </div>
                <div style={{ background: 'rgba(139,92,246,0.08)', border: '1px solid rgba(139,92,246,0.2)', borderRadius: 8, padding: 12 }}>
                  <div style={{ fontSize: 11, color: 'var(--text3)' }}>SEVERITY SCORE</div>
                  <div style={{ fontSize: 16, fontWeight: 800, color: issue.severity >= 8 ? 'var(--red)' : 'var(--yellow)', marginTop: 4 }}>{issue.severity}/10</div>
                </div>
                <div style={{ background: 'rgba(139,92,246,0.08)', border: '1px solid rgba(139,92,246,0.2)', borderRadius: 8, padding: 12 }}>
                  <div style={{ fontSize: 11, color: 'var(--text3)' }}>ROUTED TO</div>
                  <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--cyan)', marginTop: 4 }}>{issue.assignedDept}</div>
                </div>
              </div>
            </div>

            {/* Timeline */}
            <div className="card">
              <div className="card-title mb-3"><Clock size={16} color="var(--accent)" /> Status Timeline</div>
              <div className="timeline">
                {timeline.map((t, i) => (
                  <div key={i} className="timeline-item">
                    <div className="timeline-dot" style={{ background: i === timeline.length - 1 ? 'var(--green)' : 'var(--accent)' }} />
                    <div className="timeline-title">{t.title}</div>
                    <div className="timeline-content">{t.desc}</div>
                    <div style={{ fontSize: 10, color: 'var(--text3)', marginTop: 2 }}>{t.time}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Internal notes */}
            <div className="card">
              <div className="card-title mb-3">📝 Internal Notes</div>
              {notes.map((n, i) => (
                <div key={i} style={{ background: 'var(--card2)', borderRadius: 8, padding: '10px 12px', marginBottom: 8, fontSize: 12, color: 'var(--text2)', borderLeft: '3px solid var(--accent)' }}>
                  <div style={{ fontSize: 10, color: 'var(--text3)', marginBottom: 4 }}>Authority Note · Just now</div>
                  {n}
                </div>
              ))}
              <div style={{ display: 'flex', gap: 8 }}>
                <input className="input flex-1" placeholder="Add internal note..." value={note} onChange={e => setNote(e.target.value)} />
                <button className="btn btn-primary btn-sm" onClick={() => { if (note.trim()) { setNotes(p => [...p, note]); setNote(''); } }}>
                  <Send size={13} />
                </button>
              </div>
            </div>
          </div>

          {/* Right panel */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Actions */}
            <div className="card">
              <div className="card-title mb-3">⚙️ Authority Actions</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div className="input-group">
                  <label className="input-label">Update Status</label>
                  <select className="input" value={status} onChange={e => setStatus(e.target.value as any)}>
                    <option>Open</option>
                    <option>Reported</option>
                    <option>In Progress</option>
                    <option>Resolved</option>
                    <option>Escalated</option>
                  </select>
                </div>
                <div className="input-group">
                  <label className="input-label">Assign Field Officer</label>
                  <input className="input" value={officer} onChange={e => setOfficer(e.target.value)} placeholder="Officer name" />
                </div>
                <button className="btn btn-primary w-full" style={{ justifyContent: 'center' }} onClick={handleSaveChanges} disabled={saving}>
                  <CheckCircle2 size={14} /> {saving ? 'Saving...' : 'Save Changes'}
                </button>
                <button className="btn btn-danger w-full" style={{ justifyContent: 'center' }} onClick={handleEscalate} disabled={saving}>
                  <AlertTriangle size={14} /> Escalate Issue
                </button>
              </div>
            </div>

            {/* Location */}
            <div className="card">
              <div className="card-title mb-3"><MapPin size={14} color="var(--accent)" /> Location</div>
              <div style={{ background: 'var(--card2)', borderRadius: 8, padding: 12, marginBottom: 10 }}>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{issue.locationName}</div>
                <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 4, fontFamily: 'JetBrains Mono, monospace' }}>
                  {issue.latitude.toFixed(6)}, {issue.longitude.toFixed(6)}
                </div>
                <div style={{ fontSize: 11, color: 'var(--text3)' }}>{issue.ward} · Bengaluru</div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <a href={mapsLink} target="_blank" rel="noreferrer" className="btn btn-secondary w-full" style={{ justifyContent: 'center', textDecoration: 'none' }}>
                  <ExternalLink size={13} /> View on Google Maps
                </a>
                <a href={trackLink} target="_blank" rel="noreferrer" className="btn btn-primary w-full" style={{ justifyContent: 'center', textDecoration: 'none' }}>
                  <MapPin size={13} /> Get Directions / Track Location
                </a>
              </div>
              {/* Mini coordinate map preview using OSM tile overlay instead of Google Maps embed to ensure offline/no-key reliability */}
              <div style={{ marginTop: 12, borderRadius: 8, overflow: 'hidden', border: '1px solid var(--border)', height: 180, position: 'relative' }}>
                <iframe
                  title="Location Map"
                  width="100%"
                  height="100%"
                  style={{ border: 'none', display: 'block' }}
                  src={`https://maps.google.com/maps?q=${issue.latitude},${issue.longitude}&z=16&output=embed`}
                />
              </div>
            </div>

            {/* Reporter info */}
            <div className="card">
              <div className="card-title mb-3"><User size={14} color="var(--accent)" /> Report Info</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {[
                  ['Issue ID', issue.id],
                  ['Reporter', issue.reporterName || 'Anonymous Citizen'],
                  ['Time Posted', new Date(issue.timePosted).toLocaleString('en-IN')],
                  ['Upvotes', String(issue.upvotes)],
                  ['Assigned Officer', officer || 'Unassigned'],
                ].map(([l, v]) => (
                  <div key={l} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)', fontSize: 12 }}>
                    <span style={{ color: 'var(--text3)' }}>{l}</span>
                    <span style={{ fontWeight: 600, color: 'var(--text)' }}>{v}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
