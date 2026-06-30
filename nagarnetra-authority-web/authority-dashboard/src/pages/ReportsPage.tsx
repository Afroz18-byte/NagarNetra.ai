import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import type { IssueStatus, IssueCategory, Department } from '../types';
import { Search, Filter, Download, Eye, ChevronUp, ChevronDown, MapPin } from 'lucide-react';

const PAGE_SIZE = 8;

function PriorityBar({ score }: { score: number }) {
  const color = score >= 80 ? '#ef4444' : score >= 60 ? '#f59e0b' : '#10b981';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ width: 50, height: 5, background: 'var(--bg)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ width: `${score}%`, height: '100%', background: color, borderRadius: 3 }} />
      </div>
      <span style={{ fontSize: 12, fontWeight: 700, color }}>{score}</span>
    </div>
  );
}

export default function ReportsPage() {
  const navigate = useNavigate();
  const { issues } = useData();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<IssueStatus | ''>('');
  const [deptFilter, setDeptFilter] = useState<Department | ''>('');
  const [categoryFilter, setCategoryFilter] = useState<IssueCategory | ''>('');
  const [sortKey, setSortKey] = useState<'priorityScore' | 'timePosted' | 'upvotes' | 'daysOpen'>('priorityScore');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(1);

  // Compute stats on-the-fly for database models
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

  const filtered = useMemo(() => {
    return computedIssues
      .filter(i => {
        if (statusFilter && i.status !== statusFilter) return false;
        if (deptFilter && i.assignedDept !== deptFilter) return false;
        if (categoryFilter && i.category !== categoryFilter) return false;
        if (search && !i.title.toLowerCase().includes(search.toLowerCase()) && !i.locationName.toLowerCase().includes(search.toLowerCase()) && !i.id.toLowerCase().includes(search.toLowerCase())) return false;
        return true;
      })
      .sort((a, b) => {
        const va = a[sortKey] as number | string;
        const vb = b[sortKey] as number | string;
        if (typeof va === 'number' && typeof vb === 'number') return sortDir === 'desc' ? vb - va : va - vb;
        return sortDir === 'desc' ? String(vb).localeCompare(String(va)) : String(va).localeCompare(String(vb));
      });
  }, [computedIssues, search, statusFilter, deptFilter, categoryFilter, sortKey, sortDir]);

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE) || 1;
  const paged = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const sort = (k: typeof sortKey) => {
    if (k === sortKey) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortKey(k); setSortDir('desc'); }
    setPage(1);
  };

  const SortIcon = ({ k }: { k: typeof sortKey }) => {
    if (sortKey !== k) return null;
    return sortDir === 'desc' ? <ChevronDown size={12} /> : <ChevronUp size={12} />;
  };

  const getBadge = (v: string, type: 'status' | 'risk') => {
    if (type === 'status') {
      const m: Record<string, string> = { 'Open': 'badge-open', 'Reported': 'badge-open', 'In Progress': 'badge-progress', 'Resolved': 'badge-resolved', 'Escalated': 'badge-escalated' };
      return m[v] || 'badge-open';
    }
    return ({ Critical: 'badge-critical', High: 'badge-high', Medium: 'badge-medium', Low: 'badge-low' }[v] || 'badge-low');
  };

  const catEmoji: Record<string, string> = { Pothole: '🕳️', Water: '💧', Lights: '💡', Garbage: '🗑️', Sewage: '🚰', Pedestrian: '🚶', Other: '📋' };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Reports" subtitle={`${filtered.length} of ${issues.length} citizen reports`} />
      <div className="page-body fade-in">
        {/* Filters */}
        <div className="filters-bar">
          <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text3)' }} />
            <input className="input search-input" placeholder="Search by title, location, ID..." value={search} onChange={e => { setSearch(e.target.value); setPage(1); }} style={{ paddingLeft: 32 }} />
          </div>
          <select className="input" style={{ width: 140 }} value={statusFilter} onChange={e => { setStatusFilter(e.target.value as any); setPage(1); }}>
            <option value="">All Status</option>
            <option>Open</option><option>In Progress</option><option>Resolved</option><option>Escalated</option>
          </select>
          <select className="input" style={{ width: 130 }} value={deptFilter} onChange={e => { setDeptFilter(e.target.value as any); setPage(1); }}>
            <option value="">All Depts</option>
            <option>BBMP</option><option>PWD</option><option>BWSSB</option><option>BESCOM</option>
          </select>
          <select className="input" style={{ width: 140 }} value={categoryFilter} onChange={e => { setCategoryFilter(e.target.value as any); setPage(1); }}>
            <option value="">All Categories</option>
            <option>Pothole</option><option>Water</option><option>Lights</option><option>Garbage</option><option>Sewage</option><option>Pedestrian</option><option>Other</option>
          </select>
          <button className="btn btn-secondary btn-sm"><Filter size={14} /> Filters</button>
          <button className="btn btn-secondary btn-sm" onClick={() => {
            const csv = [
              ['ID', 'Title', 'Category', 'Priority Score', 'Risk Level', 'Status', 'Department', 'Location', 'Upvotes', 'Days Open'].join(','),
              ...filtered.map(i => [
                i.id,
                `"${i.title.replace(/"/g, '""')}"`,
                i.category,
                i.priorityScore,
                i.riskLevel,
                i.status,
                i.assignedDept,
                `"${i.locationName.replace(/"/g, '""')}"`,
                i.upvotes,
                i.daysOpen
              ].join(','))
            ].join('\n');
            const blob = new Blob([csv], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.setAttribute('href', url);
            a.setAttribute('download', `nagarnetra_reports_${Date.now()}.csv`);
            a.click();
          }}><Download size={14} /> Export CSV</button>
        </div>

        {/* Table */}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Issue</th>
                <th>Category</th>
                <th className="sortable" onClick={() => sort('priorityScore')}>Priority <SortIcon k="priorityScore" /></th>
                <th>Risk</th>
                <th>Status</th>
                <th>Dept</th>
                <th>Location</th>
                <th className="sortable" onClick={() => sort('upvotes')}>Upvotes <SortIcon k="upvotes" /></th>
                <th className="sortable" onClick={() => sort('daysOpen')}>Days Open <SortIcon k="daysOpen" /></th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paged.map(issue => (
                <tr key={issue.id} onClick={() => navigate(`/reports/${issue.id}`)}>
                  <td><span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 11, color: 'var(--accent)', fontWeight: 600 }}>{issue.id}</span></td>
                  <td style={{ maxWidth: 220 }}>
                    <div style={{ fontWeight: 600, fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{issue.title}</div>
                    <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 2 }}>{issue.ward}</div>
                  </td>
                  <td><span style={{ fontSize: 12 }}>{catEmoji[issue.category] || '📋'} {issue.category}</span></td>
                  <td><PriorityBar score={issue.priorityScore} /></td>
                  <td><span className={`badge ${getBadge(issue.riskLevel, 'risk')}`}>{issue.riskLevel}</span></td>
                  <td><span className={`badge ${getBadge(issue.status, 'status')}`}>{issue.status}</span></td>
                  <td><span style={{ fontSize: 11, fontWeight: 600, color: 'var(--accent)' }}>{issue.assignedDept}</span></td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 11, color: 'var(--text2)', maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      <MapPin size={10} />{issue.locationName}
                    </div>
                  </td>
                  <td><span style={{ fontWeight: 700, color: issue.upvotes > 40 ? 'var(--red)' : 'var(--text2)' }}>▲ {issue.upvotes}</span></td>
                  <td>
                    <span style={{ fontWeight: 700, color: issue.daysOpen > 6 ? 'var(--red)' : issue.daysOpen > 3 ? 'var(--yellow)' : 'var(--green)' }}>
                      {issue.daysOpen}d
                    </span>
                  </td>
                  <td onClick={e => e.stopPropagation()}>
                    <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/reports/${issue.id}`)}>
                      <Eye size={13} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 16, padding: '0 4px' }}>
          <span style={{ fontSize: 12, color: 'var(--text3)' }}>
            Showing {filtered.length > 0 ? (page - 1) * PAGE_SIZE + 1 : 0}–{Math.min(page * PAGE_SIZE, filtered.length)} of {filtered.length} reports
          </span>
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-secondary btn-sm" disabled={page === 1} onClick={() => setPage(p => p - 1)}>← Prev</button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
              <button key={p} className={`btn btn-sm ${page === p ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setPage(p)}>{p}</button>
            ))}
            <button className="btn btn-secondary btn-sm" disabled={page === totalPages} onClick={() => setPage(p => p + 1)}>Next →</button>
          </div>
        </div>
      </div>
    </div>
  );
}
