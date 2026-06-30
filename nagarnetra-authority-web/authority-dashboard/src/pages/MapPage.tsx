import { useEffect, useMemo, useRef, useState } from 'react';
import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import { useNavigate } from 'react-router-dom';
import type { IssueCategory, IssueStatus } from '../types';
import { Sun, Moon } from 'lucide-react';
import { buildIssueClusters } from '../lib/issueClustering';

declare const L: any;

export default function MapPage() {
  const { issues } = useData();
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<any>(null);
  const markersGroupRef = useRef<any>(null);
  const clusterCircleGroupRef = useRef<any>(null);
  const [isDarkMap, setIsDarkMap] = useState(true);
  const [statusFilter, setStatusFilter] = useState<IssueStatus | ''>('');
  const [categoryFilter, setCategoryFilter] = useState<IssueCategory | ''>('');
  const [selected, setSelected] = useState<string | null>(null);
  const navigate = useNavigate();

  // Expose global callback for popup details buttons
  useEffect(() => {
    (window as any).viewIssueDetails = (id: string) => {
      navigate(`/reports/${id}`);
    };
    return () => {
      delete (window as any).viewIssueDetails;
    };
  }, [navigate]);

  const filtered = issues.filter(i => {
    if (statusFilter && i.status !== statusFilter) return false;
    if (categoryFilter && i.category !== categoryFilter) return false;
    return true;
  });

  const selectedIssue = issues.find(i => i.id === selected);
  const visibleClusters = useMemo(() => buildIssueClusters(filtered), [filtered]);

  const getCategoryColor = (category: string) => {
    const c = category.toLowerCase();
    if (c.includes("water") || c.includes("lake"))        return "#3B82F6";
    if (c.includes("light") || c.includes("bescom"))      return "#F59E0B";
    if (c.includes("garbage") || c.includes("trash"))     return "#F97316";
    if (c.includes("pothole") || c.includes("road"))      return "#8B5CF6";
    if (c.includes("sewage") || c.includes("drain"))      return "#B45309";
    return "#EF4444";
  };

  const getStatusBadgeColor = (status: string) => {
    if (!status) return "#64748B";
    const s = status.toLowerCase();
    if (s.includes("resolved"))   return "#22C55E";
    if (s.includes("progress"))   return "#3B82F6";
    if (s.includes("urgent") || s.includes("escalat")) return "#EF4444";
    if (s.includes("verified"))   return "#6366F1";
    return "#F59E0B";
  };

  const getSVGIcon = (category: string, severity: number) => {
    const color = getCategoryColor(category);
    const size = severity >= 8 ? 38 : severity >= 6 ? 32 : 26;
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${color}" width="${size}" height="${size}">
      <filter id="shadow"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.4"/></filter>
      <path filter="url(#shadow)" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
    </svg>`;
    
    return L.divIcon({
      html: svg,
      className: 'custom-svg-marker',
      iconSize: [size, size],
      iconAnchor: [size / 2, size],
      popupAnchor: [0, -size]
    });
  };

  // Initialize Map
  useEffect(() => {
    if (!mapRef.current || mapInstance.current) return;
    if (typeof L === 'undefined') return;

    mapInstance.current = L.map(mapRef.current, {
      center: [12.9716, 77.5946],
      zoom: 12,
      zoomControl: true,
    });

    const darkTile = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
    const lightTile = 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
    
    L.tileLayer(isDarkMap ? darkTile : lightTile, { 
      attribution: '© OpenStreetMap, © CARTO', 
      maxZoom: 19 
    }).addTo(mapInstance.current);

    markersGroupRef.current = L.markerClusterGroup({
      maxClusterRadius: 60,
      showCoverageOnHover: false,
      spiderfyOnMaxZoom: true
    });
    clusterCircleGroupRef.current = L.layerGroup();
    mapInstance.current.addLayer(clusterCircleGroupRef.current);
    mapInstance.current.addLayer(markersGroupRef.current);

    return () => {
      mapInstance.current?.remove();
      mapInstance.current = null;
    };
  }, []);

  // Update tiles when Dark/Light changes
  useEffect(() => {
    if (!mapInstance.current) return;
    const darkTile = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
    const lightTile = 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
    
    // Remove existing tile layer and add new one
    mapInstance.current.eachLayer((layer: any) => {
      if (layer instanceof L.TileLayer) {
        mapInstance.current.removeLayer(layer);
      }
    });

    L.tileLayer(isDarkMap ? darkTile : lightTile, { 
      attribution: '© OpenStreetMap, © CARTO', 
      maxZoom: 19 
    }).addTo(mapInstance.current);
  }, [isDarkMap]);

  // Update Markers
  useEffect(() => {
    if (!mapInstance.current || !markersGroupRef.current || typeof L === 'undefined') return;
    
    markersGroupRef.current.clearLayers();
    clusterCircleGroupRef.current?.clearLayers();

    visibleClusters.forEach(cluster => {
      const color = getCategoryColor(cluster.category);
      const radius = Math.min(58, 24 + cluster.count * 5);
      const circle = L.circleMarker([cluster.latitude, cluster.longitude], {
        radius,
        color,
        weight: 2,
        fillColor: color,
        fillOpacity: isDarkMap ? 0.28 : 0.2,
        opacity: 0.85,
      });
      const label = L.divIcon({
        html: `<div class="map-cluster-count" style="border-color:${color};box-shadow:0 0 0 7px ${color}22;">${cluster.count}</div>`,
        className: 'map-cluster-label',
        iconSize: [36, 36],
        iconAnchor: [18, 18],
      });
      const marker = L.marker([cluster.latitude, cluster.longitude], { icon: label, interactive: false });
      circle.bindPopup(`
        <div style="font-family:Inter,sans-serif;padding:6px;width:210px;">
          <h4 style="margin:0 0 6px 0;font-size:13px;">${cluster.count} grouped ${cluster.category} reports</h4>
          <p style="margin:0 0 6px 0;font-size:11px;color:#64748b;">${cluster.locationName}</p>
          <p style="margin:0;font-size:11px;color:#64748b;">Priority ${cluster.priorityScore}/100 - ${cluster.riskLevel}</p>
        </div>
      `);
      clusterCircleGroupRef.current.addLayer(circle);
      clusterCircleGroupRef.current.addLayer(marker);
    });

    filtered.forEach(issue => {
      const marker = L.marker([issue.latitude, issue.longitude], {
        icon: getSVGIcon(issue.category, issue.severity || 5)
      });

      let imgTag = "";
      if (issue.photoBase64 && issue.photoBase64.length > 20) {
        const src = issue.photoBase64.startsWith('data:') ? issue.photoBase64 : `data:image/jpeg;base64,${issue.photoBase64}`;
        imgTag = `<img src="${src}" style="width:100%;height:auto;max-height:110px;border-radius:10px;margin:8px 0;object-fit:cover;display:block;" />`;
      }

      const statusColor = getStatusBadgeColor(issue.status);
      const catColor = getCategoryColor(issue.category);
      const popupTextColor = isDarkMap ? "#f8fafc" : "#0f172a";
      const popupSubColor = isDarkMap ? "#94a3b8" : "#475569";

      const infoHtml = `
        <div style="color:${popupTextColor};font-family:Inter,sans-serif;padding:6px;width:210px;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
            <span style="font-size:9px;background:${catColor}22;color:${catColor};padding:3px 9px;border-radius:12px;font-weight:bold;text-transform:uppercase;border:1px solid ${catColor}44;">${issue.category || 'Issue'}</span>
            <span style="font-size:9px;background:${statusColor}22;color:${statusColor};padding:2px 7px;border-radius:10px;font-weight:bold;">${issue.status || ''}</span>
          </div>
          <h4 style="margin:0 0 3px 0;font-size:13px;font-weight:bold;color:${popupTextColor};line-height:1.3;">${issue.title}</h4>
          <p style="margin:0 0 5px 0;font-size:10px;color:${popupSubColor};">📍 ${issue.locationName}</p>
          ${imgTag}
          <p style="margin:0 0 8px 0;font-size:11px;color:${popupSubColor};line-height:1.5;">${(issue.description || '').substring(0, 100)}${issue.description && issue.description.length > 100 ? '...' : ''}</p>
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
            <span style="font-size:10px;color:#FF6B00;font-weight:bold;">👥 ${issue.upvotes || 0} citizens</span>
            <span style="font-size:10px;color:${issue.severity >= 8 ? '#EF4444' : issue.severity >= 6 ? '#F97316' : '#22C55E'};font-weight:bold;">⚡ Sev: ${issue.severity || 5}/10</span>
          </div>
          <button onclick="window.viewIssueDetails('${issue.id}')" style="width:100%;height:34px;background:linear-gradient(135deg,#FF6B00,#FF8C42);color:white;border:none;border-radius:8px;font-size:12px;font-weight:bold;cursor:pointer;letter-spacing:0.5px;">VIEW DETAILS →</button>
        </div>
      `;

      marker.bindPopup(infoHtml, { maxWidth: 230 });
      marker.on('click', () => setSelected(issue.id));
      markersGroupRef.current.addLayer(marker);
    });
  }, [filtered, visibleClusters, isDarkMap]);

  const getBadge = (v: string) => ({ 'Open': 'badge-open', 'In Progress': 'badge-progress', 'Resolved': 'badge-resolved', 'Escalated': 'badge-escalated' }[v] || 'badge-open');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Live Map View" subtitle={`${filtered.length} active issues mapped - ${visibleClusters.length} clusters - Bengaluru`} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', padding: '16px' }}>
        {/* Controls */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <select className="input" style={{ width: 140 }} value={statusFilter} onChange={e => setStatusFilter(e.target.value as any)}>
            <option value="">All Status</option>
            <option>Open</option><option>In Progress</option><option>Resolved</option><option>Escalated</option>
          </select>
          <select className="input" style={{ width: 140 }} value={categoryFilter} onChange={e => setCategoryFilter(e.target.value as any)}>
            <option value="">All Categories</option>
            <option>Pothole</option><option>Water</option><option>Lights</option><option>Garbage</option><option>Sewage</option><option>Pedestrian</option>
          </select>
          <button className="btn btn-secondary btn-sm" onClick={() => setIsDarkMap(!isDarkMap)}>
            {isDarkMap ? <Sun size={13} /> : <Moon size={13} />} {isDarkMap ? 'Light Map' : 'Dark Map'}
          </button>
          {/* Legend */}
          <div style={{ display: 'flex', gap: 12, marginLeft: 'auto', alignItems: 'center' }}>
            {[['#8B5CF6', 'Potholes / Roadways'], ['#3B82F6', 'Water Issues'], ['#F59E0B', 'Streetlights'], ['#F97316', 'Garbage / Trash'], ['#B45309', 'Sewage / Drainage']].map(([c, l]) => (
              <div key={l} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 11, color: 'var(--text2)' }}>
                <div style={{ width: 10, height: 10, borderRadius: '50%', background: c, border: '1px solid rgba(255,255,255,0.3)' }} />{l}
              </div>
            ))}
          </div>
        </div>

        {/* Map + Side panel */}
        <div style={{ display: 'flex', gap: 16, flex: 1, minHeight: 0 }}>
          <div ref={mapRef} style={{ flex: 1, borderRadius: 12, border: '1px solid var(--border)', overflow: 'hidden', minHeight: 400 }} />

          {/* Issue list sidebar */}
          <div style={{ width: 280, background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ padding: '12px 14px', borderBottom: '1px solid var(--border)', fontSize: 12, fontWeight: 700, color: 'var(--text)' }}>
              {filtered.length} Issues on Map - {visibleClusters.length} Clusters
            </div>
            <div style={{ flex: 1, overflowY: 'auto' }}>
              {filtered.map(issue => (
                <div key={issue.id}
                  onClick={() => { setSelected(issue.id); mapInstance.current?.flyTo([issue.latitude, issue.longitude], 15, { duration: 1 }); }}
                  style={{ padding: '10px 14px', borderBottom: '1px solid var(--border)', cursor: 'pointer', background: selected === issue.id ? 'var(--card2)' : 'transparent', transition: 'background 0.15s', borderLeft: selected === issue.id ? '3px solid var(--accent)' : '3px solid transparent' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 12, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{issue.title}</div>
                      <div style={{ fontSize: 10, color: 'var(--text3)', marginTop: 2 }}>{issue.locationName}</div>
                    </div>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: getCategoryColor(issue.category), flexShrink: 0, marginTop: 3, marginLeft: 6 }} />
                  </div>
                  <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                    <span className={`badge ${getBadge(issue.status)}`}>{issue.status}</span>
                    <span style={{ fontSize: 10, color: 'var(--text3)' }}>Sev: {issue.severity}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Selected issue detail */}
        {selectedIssue && (
          <div style={{ marginTop: 12, background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: '14px 18px', display: 'flex', gap: 20, alignItems: 'center', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: 200 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>{selectedIssue.title}</div>
              <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 2 }}>{selectedIssue.locationName} · {selectedIssue.ward}</div>
            </div>
            <div style={{ fontSize: 11, color: 'var(--text2)', fontFamily: 'JetBrains Mono, monospace' }}>
              📍 {selectedIssue.latitude.toFixed(6)}, {selectedIssue.longitude.toFixed(6)}
            </div>
            <span className={`badge badge-${(selectedIssue.severity >= 8 ? 'high' : selectedIssue.severity >= 5 ? 'medium' : 'low')}`}>{selectedIssue.severity >= 8 ? 'High' : selectedIssue.severity >= 5 ? 'Medium' : 'Low'} Severity</span>
            <a href={`https://www.google.com/maps/dir//${selectedIssue.latitude},${selectedIssue.longitude}`} target="_blank" rel="noreferrer" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>
              🗺️ Get Directions
            </a>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/reports/${selectedIssue.id}`)}>View Full Report</button>
            <button className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>✕</button>
          </div>
        )}
      </div>
    </div>
  );
}
