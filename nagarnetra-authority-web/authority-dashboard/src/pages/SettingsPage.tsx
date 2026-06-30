import Header from '../components/Header';
import { useState } from 'react';
import { Save, Shield, Bell, Database, Sliders, Users, CheckCircle2 } from 'lucide-react';

export default function SettingsPage() {
  const [saved, setSaved] = useState(false);
  const [slaThreshold, setSlaThreshold] = useState(7);
  const [highPriorityScore, setHighPriorityScore] = useState(80);
  const [medPriorityScore, setMedPriorityScore] = useState(50);
  const [apiKey, setApiKey] = useState('••••••••••••••••••••••••••••••••••••••••');
  const [tab, setTab] = useState('general');

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const tabs = [
    { id: 'general', label: 'General', icon: Sliders },
    { id: 'users', label: 'Users & Roles', icon: Users },
    { id: 'escalation', label: 'Escalation', icon: Bell },
    { id: 'api', label: 'API & System', icon: Database },
    { id: 'security', label: 'Security', icon: Shield },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Settings" subtitle="Configure NagarNetra Authority Panel" />
      <div className="page-body fade-in">
        {saved && (
          <div style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.3)', borderRadius: 10, padding: '12px 16px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
            <CheckCircle2 size={16} color="var(--green)" />
            <span style={{ fontSize: 13, color: 'var(--green)', fontWeight: 600 }}>Settings saved successfully!</span>
          </div>
        )}

        <div style={{ display: 'flex', gap: 20 }}>
          {/* Tab sidebar */}
          <div style={{ width: 200, background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: '12px 8px', height: 'fit-content' }}>
            {tabs.map(({ id, label, icon: Icon }) => (
              <button key={id} onClick={() => setTab(id)} className={`nav-item ${tab === id ? 'active' : ''}`} style={{ width: '100%', textAlign: 'left' }}>
                <Icon size={16} /> {label}
              </button>
            ))}
          </div>

          {/* Content */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 16 }}>
            {tab === 'general' && (
              <div className="card">
                <div className="card-title mb-4">⚙️ General Settings</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {[
                    { l: 'Authority Name', v: 'Bruhat Bengaluru Mahanagara Palike' },
                    { l: 'City', v: 'Bengaluru, Karnataka' },
                    { l: 'Admin Email', v: 'admin@bbmp.gov.in' },
                    { l: 'Contact Phone', v: '+91 80 2221 8888' },
                  ].map(({ l, v }) => (
                    <div key={l} className="input-group">
                      <label className="input-label">{l}</label>
                      <input className="input" defaultValue={v} />
                    </div>
                  ))}
                  <div className="input-group">
                    <label className="input-label">Timezone</label>
                    <select className="input"><option>Asia/Kolkata (IST +5:30)</option></select>
                  </div>
                  <div className="input-group">
                    <label className="input-label">Language</label>
                    <select className="input"><option>English</option><option>Kannada</option><option>Hindi</option></select>
                  </div>
                </div>
              </div>
            )}

            {tab === 'escalation' && (
              <div className="card">
                <div className="card-title mb-4">🚨 Escalation & Priority Settings</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                  <div className="input-group">
                    <label className="input-label">SLA Escalation Threshold (days)</label>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <input type="range" min={1} max={30} value={slaThreshold} onChange={e => setSlaThreshold(Number(e.target.value))} style={{ flex: 1 }} />
                      <span style={{ width: 40, textAlign: 'center', fontWeight: 700, color: 'var(--accent)', fontSize: 18 }}>{slaThreshold}</span>
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text3)' }}>Issues unresolved after {slaThreshold} days will be automatically escalated by EscalationBot</div>
                  </div>
                  <div className="input-group">
                    <label className="input-label">High Priority Score Threshold</label>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <input type="range" min={50} max={100} value={highPriorityScore} onChange={e => setHighPriorityScore(Number(e.target.value))} style={{ flex: 1 }} />
                      <span style={{ width: 40, textAlign: 'center', fontWeight: 700, color: 'var(--red)', fontSize: 18 }}>{highPriorityScore}</span>
                    </div>
                  </div>
                  <div className="input-group">
                    <label className="input-label">Medium Priority Score Threshold</label>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <input type="range" min={20} max={80} value={medPriorityScore} onChange={e => setMedPriorityScore(Number(e.target.value))} style={{ flex: 1 }} />
                      <span style={{ width: 40, textAlign: 'center', fontWeight: 700, color: 'var(--yellow)', fontSize: 18 }}>{medPriorityScore}</span>
                    </div>
                  </div>
                  <div style={{ background: 'var(--card2)', borderRadius: 8, padding: 12, fontSize: 12, color: 'var(--text2)' }}>
                    <strong>Score Guide:</strong><br />
                    🔴 High: {highPriorityScore}–100 · 🟡 Medium: {medPriorityScore}–{highPriorityScore - 1} · 🟢 Low: 0–{medPriorityScore - 1}
                  </div>

                  <div className="input-group">
                    <label className="input-label">Escalation Email Recipients</label>
                    {['BBMP', 'PWD', 'BWSSB', 'BESCOM'].map(dept => (
                      <div key={dept} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                        <span style={{ width: 70, fontSize: 12, fontWeight: 700, color: 'var(--accent)', paddingTop: 8 }}>{dept}</span>
                        <input className="input flex-1" defaultValue={`head@${dept.toLowerCase()}.gov.in`} />
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {tab === 'users' && (
              <div className="card">
                <div className="card-title mb-4">👥 Authority Users</div>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr><th>Name</th><th>Email</th><th>Role</th><th>Department</th><th>Status</th><th>Actions</th></tr>
                    </thead>
                    <tbody>
                      {[
                        { name: 'Shri Arjun Sharma', email: 'admin@nagarnetra.gov.in', role: 'Super Admin', dept: 'All', status: 'Active' },
                        { name: 'Smt. Priya Nair', email: 'bbmp@nagarnetra.gov.in', role: 'Department Admin', dept: 'BBMP', status: 'Active' },
                        { name: 'Shri Rajesh Kumar', email: 'officer@nagarnetra.gov.in', role: 'Field Officer', dept: 'PWD', status: 'Active' },
                        { name: 'Smt. Anitha Reddy', email: 'anitha@bwssb.gov.in', role: 'Field Officer', dept: 'BWSSB', status: 'Active' },
                      ].map(u => (
                        <tr key={u.email}>
                          <td style={{ fontWeight: 600, fontSize: 12 }}>{u.name}</td>
                          <td style={{ fontSize: 11, color: 'var(--text3)' }}>{u.email}</td>
                          <td><span className="badge badge-open">{u.role}</span></td>
                          <td style={{ fontSize: 12, fontWeight: 700, color: 'var(--accent)' }}>{u.dept}</td>
                          <td><span className="badge badge-resolved">{u.status}</span></td>
                          <td><button className="btn btn-ghost btn-sm">Edit</button></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <button className="btn btn-primary mt-4"><Users size={14} /> + Add User</button>
              </div>
            )}

            {tab === 'api' && (
              <div className="card">
                <div className="card-title mb-4">🔌 API & System Configuration</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <div className="input-group">
                    <label className="input-label">Gemini API Key</label>
                    <input className="input" type="password" value={apiKey} onChange={e => setApiKey(e.target.value)} />
                    <div style={{ fontSize: 11, color: 'var(--text3)' }}>Used by all 8 AI agents for image analysis and chat</div>
                  </div>
                  <div className="input-group">
                    <label className="input-label">Google Maps API Key</label>
                    <input className="input" type="password" defaultValue="••••••••••••••••••••••••••••••" />
                  </div>
                  <div className="input-group">
                    <label className="input-label">Firebase Project ID</label>
                    <input className="input" defaultValue="nagarnetra-ai" />
                  </div>
                  <div className="input-group">
                    <label className="input-label">Backend API URL</label>
                    <input className="input" defaultValue="https://api.nagarnetra.gov.in/v1" />
                  </div>
                  <div style={{ background: 'rgba(16,185,129,0.08)', border: '1px solid rgba(16,185,129,0.2)', borderRadius: 8, padding: 12 }}>
                    <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--green)', marginBottom: 4 }}>✅ System Status: All services operational</div>
                    <div style={{ fontSize: 11, color: 'var(--text3)' }}>Gemini API: Connected · Firebase: Connected · Maps: Connected</div>
                  </div>
                </div>
              </div>
            )}

            {tab === 'security' && (
              <div className="card">
                <div className="card-title mb-4">🔒 Security Settings</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  {[
                    ['Two-Factor Authentication', true],
                    ['Session Timeout (30 min)', true],
                    ['IP Whitelist Enforcement', false],
                    ['Audit Log Enabled', true],
                    ['Require Strong Password', true],
                  ].map(([l, v]) => (
                    <div key={String(l)} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 14px', background: 'var(--card2)', borderRadius: 8 }}>
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 600 }}>{l}</div>
                      </div>
                      <div style={{ width: 40, height: 22, borderRadius: 11, background: v ? 'var(--green)' : 'var(--border)', position: 'relative', cursor: 'pointer', flexShrink: 0 }}>
                        <div style={{ position: 'absolute', top: 3, left: v ? 20 : 3, width: 16, height: 16, borderRadius: '50%', background: '#fff', transition: 'left 0.2s' }} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <button className="btn btn-primary" onClick={handleSave} style={{ alignSelf: 'flex-start', gap: 8 }}>
              <Save size={14} /> Save Settings
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
