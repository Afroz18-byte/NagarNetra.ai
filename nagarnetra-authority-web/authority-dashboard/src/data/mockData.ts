import type { Issue, DepartmentStat, AgentActivity, Notification, WardStat } from '../types';

export const MOCK_ISSUES: Issue[] = [
  { id: 'NN-001', title: 'Large pothole on 100ft Road', description: 'Deep pothole causing accidents near Indiranagar 100ft road junction. Multiple vehicles damaged.', category: 'Pothole', status: 'Escalated', severity: 9, riskLevel: 'Critical', assignedDept: 'PWD', locationName: 'Indiranagar 100ft Road', latitude: 12.9784, longitude: 77.6408, upvotes: 47, timePosted: '2026-06-18T08:30:00', ward: 'Ward 80', slaStatus: 'Overdue', priorityScore: 94, daysOpen: 7, assignedOfficer: 'Rajesh Kumar', aiConfidence: 97, escalationReason: 'Unresolved for 7 days with high upvotes' },
  { id: 'NN-002', title: 'Street light outage — 6 consecutive lights', description: 'Six street lights not working on Koramangala 5th Block main road creating safety hazard at night.', category: 'Lights', status: 'In Progress', severity: 7, riskLevel: 'High', assignedDept: 'BESCOM', locationName: 'Koramangala 5th Block', latitude: 12.9352, longitude: 77.6245, upvotes: 31, timePosted: '2026-06-20T11:00:00', ward: 'Ward 68', slaStatus: 'At Risk', priorityScore: 78, daysOpen: 5, assignedOfficer: 'Priya Sharma', aiConfidence: 92 },
  { id: 'NN-003', title: 'Water pipeline burst — flooding road', description: 'Major pipeline burst near Whitefield main road causing flooding and traffic disruption.', category: 'Water', status: 'Open', severity: 10, riskLevel: 'Critical', assignedDept: 'BWSSB', locationName: 'Whitefield Main Road', latitude: 12.9698, longitude: 77.7499, upvotes: 63, timePosted: '2026-06-25T06:15:00', ward: 'Ward 49', slaStatus: 'Due Today', priorityScore: 98, daysOpen: 0, aiConfidence: 99, reporterName: 'Suresh Menon' },
  { id: 'NN-004', title: 'Garbage overflowing from bin — 3 days', description: 'Municipal garbage bins overflowing near HSR Layout sector 2 market causing health hazard.', category: 'Garbage', status: 'Open', severity: 6, riskLevel: 'Medium', assignedDept: 'BBMP', locationName: 'HSR Layout Sector 2', latitude: 12.9116, longitude: 77.6389, upvotes: 22, timePosted: '2026-06-23T09:00:00', ward: 'Ward 174', slaStatus: 'On Track', priorityScore: 61, daysOpen: 2, aiConfidence: 88 },
  { id: 'NN-005', title: 'Broken footpath — tripping hazard', description: 'Multiple broken footpath slabs near Jayanagar 4th Block creating hazard for elderly and children.', category: 'Pedestrian', status: 'Resolved', severity: 4, riskLevel: 'Medium', assignedDept: 'BBMP', locationName: 'Jayanagar 4th Block', latitude: 12.9300, longitude: 77.5831, upvotes: 15, timePosted: '2026-06-17T14:00:00', ward: 'Ward 147', slaStatus: 'Resolved', priorityScore: 44, daysOpen: 8, resolutionNotes: 'Footpath repaired and relaid.', aiConfidence: 85 },
  { id: 'NN-006', title: 'Open manhole cover — danger to motorcyclists', description: 'Open manhole on MG Road near Trinity Circle. Very dangerous especially during night.', category: 'Sewage', status: 'In Progress', severity: 9, riskLevel: 'Critical', assignedDept: 'BWSSB', locationName: 'MG Road, Trinity Circle', latitude: 12.9758, longitude: 77.6068, upvotes: 58, timePosted: '2026-06-22T17:30:00', ward: 'Ward 76', slaStatus: 'At Risk', priorityScore: 91, daysOpen: 3, assignedOfficer: 'Mohammed Irfan', aiConfidence: 96 },
  { id: 'NN-007', title: 'Sewage overflow on residential road', description: 'Raw sewage overflowing on 5th Cross, Malleshwaram. Foul smell and health risk to residents.', category: 'Sewage', status: 'Escalated', severity: 8, riskLevel: 'High', assignedDept: 'BWSSB', locationName: 'Malleshwaram 5th Cross', latitude: 13.0035, longitude: 77.5682, upvotes: 39, timePosted: '2026-06-17T10:00:00', ward: 'Ward 28', slaStatus: 'Overdue', priorityScore: 87, daysOpen: 8, escalationReason: 'SLA exceeded — health hazard', aiConfidence: 94 },
  { id: 'NN-008', title: 'Road cave-in — half lane blocked', description: 'Road cave-in blocking half of Bannerghatta Road near NIMHANS. Causing heavy traffic congestion.', category: 'Pothole', status: 'Open', severity: 8, riskLevel: 'High', assignedDept: 'PWD', locationName: 'Bannerghatta Road, NIMHANS', latitude: 12.9432, longitude: 77.5940, upvotes: 44, timePosted: '2026-06-24T07:00:00', ward: 'Ward 152', slaStatus: 'Due Today', priorityScore: 88, daysOpen: 1, aiConfidence: 91 },
  { id: 'NN-009', title: 'No water supply for 48 hours', description: 'Entire apartment complex in Electronic City Phase 2 without water supply for 2 days.', category: 'Water', status: 'In Progress', severity: 8, riskLevel: 'High', assignedDept: 'BWSSB', locationName: 'Electronic City Phase 2', latitude: 12.8399, longitude: 77.6770, upvotes: 71, timePosted: '2026-06-23T18:00:00', ward: 'Ward 196', slaStatus: 'At Risk', priorityScore: 85, daysOpen: 2, assignedOfficer: 'Anitha Reddy', aiConfidence: 89 },
  { id: 'NN-010', title: 'Illegal garbage dumping — burning waste', description: 'Unauthorized garbage dump point in Bellandur with burning waste. Air pollution and health hazard.', category: 'Garbage', status: 'Open', severity: 7, riskLevel: 'High', assignedDept: 'BBMP', locationName: 'Bellandur Lake Road', latitude: 12.9258, longitude: 77.6660, upvotes: 33, timePosted: '2026-06-24T11:00:00', ward: 'Ward 150', slaStatus: 'On Track', priorityScore: 74, daysOpen: 1, aiConfidence: 83 },
  { id: 'NN-011', title: 'Transformer malfunction — area blackout', description: 'Transformer failure causing complete blackout in Rajajinagar 4th Block. 200+ houses affected.', category: 'Lights', status: 'Resolved', severity: 8, riskLevel: 'High', assignedDept: 'BESCOM', locationName: 'Rajajinagar 4th Block', latitude: 12.9916, longitude: 77.5530, upvotes: 27, timePosted: '2026-06-20T20:00:00', ward: 'Ward 29', slaStatus: 'Resolved', priorityScore: 82, daysOpen: 5, resolutionNotes: 'Transformer replaced. Power restored.', aiConfidence: 95 },
  { id: 'NN-012', title: 'Waterlogging during rain near underpass', description: 'Silk Board underpass floods completely during rain making it impassable.', category: 'Water', status: 'Open', severity: 7, riskLevel: 'High', assignedDept: 'BBMP', locationName: 'Silk Board Junction', latitude: 12.9172, longitude: 77.6226, upvotes: 52, timePosted: '2026-06-25T05:30:00', ward: 'Ward 173', slaStatus: 'Due Today', priorityScore: 80, daysOpen: 0, aiConfidence: 90 },
  { id: 'NN-013', title: 'Missing road divider — accident risk', description: 'Road divider missing for 200m stretch on Outer Ring Road near Marathahalli causing head-on risks.', category: 'Pedestrian', status: 'Open', severity: 6, riskLevel: 'Medium', assignedDept: 'PWD', locationName: 'Outer Ring Road, Marathahalli', latitude: 12.9591, longitude: 77.6984, upvotes: 19, timePosted: '2026-06-24T13:00:00', ward: 'Ward 85', slaStatus: 'On Track', priorityScore: 65, daysOpen: 1, aiConfidence: 78 },
  { id: 'NN-014', title: 'Drinking water contamination reported', description: 'Residents in Yelahanka report foul smell and brownish color in tap water. Contamination suspected.', category: 'Water', status: 'Escalated', severity: 9, riskLevel: 'Critical', assignedDept: 'BWSSB', locationName: 'Yelahanka New Town', latitude: 13.1007, longitude: 77.5963, upvotes: 81, timePosted: '2026-06-19T09:00:00', ward: 'Ward 4', slaStatus: 'Overdue', priorityScore: 96, daysOpen: 6, escalationReason: 'Health emergency — water contamination', aiConfidence: 97 },
  { id: 'NN-015', title: 'Blocked storm drain causing flooding', description: 'Storm drain completely blocked near Hebbal flyover. Water flooding during any rain.', category: 'Sewage', status: 'In Progress', severity: 6, riskLevel: 'Medium', assignedDept: 'BBMP', locationName: 'Hebbal Flyover', latitude: 13.0353, longitude: 77.5970, upvotes: 28, timePosted: '2026-06-22T14:00:00', ward: 'Ward 7', slaStatus: 'On Track', priorityScore: 67, daysOpen: 3, assignedOfficer: 'Deepak Nair', aiConfidence: 86 },
];

export const DEPARTMENT_STATS: DepartmentStat[] = [
  { name: 'BBMP', displayName: 'Bruhat Bengaluru Mahanagara Palike', totalIssues: 142, resolved: 89, pending: 41, escalated: 12, avgResolutionDays: 4.2, head: 'Shri Tushar Giri Nath', color: '#10b981' },
  { name: 'PWD', displayName: 'Public Works Department', totalIssues: 98, resolved: 54, pending: 34, escalated: 10, avgResolutionDays: 5.8, head: 'Shri Lokesh Kumar', color: '#3b82f6' },
  { name: 'BWSSB', displayName: 'Bangalore Water Supply & Sewerage Board', totalIssues: 87, resolved: 41, pending: 35, escalated: 11, avgResolutionDays: 3.9, head: 'Shri N. Jayaram', color: '#06b6d4' },
  { name: 'BESCOM', displayName: 'Bangalore Electricity Supply Company', totalIssues: 63, resolved: 48, pending: 12, escalated: 3, avgResolutionDays: 2.1, head: 'Shri Mahesh Babu', color: '#f59e0b' },
];

export const AGENT_ACTIVITIES: AgentActivity[] = [
  { id: 'a1', name: 'OrchestratorBot', role: 'Master Coordinator', status: 'Active', lastAction: 'Coordinated issue NN-003 — assigned to BWSSB with Critical priority', actionsToday: 47, totalActions: 1842, lastActionTime: '2 min ago', icon: '🤖', color: '#8b5cf6', recentLogs: ['Routed NN-003 to BWSSB', 'Escalated NN-001 after 7 days', 'Merged duplicate reports NN-011 & NN-016', 'Triggered escalation chain for Ward 80'] },
  { id: 'a2', name: 'VisionBot', role: 'Image Analyst', status: 'Active', lastAction: 'Analyzed pothole image — severity 9/10, confidence 97%', actionsToday: 31, totalActions: 984, lastActionTime: '5 min ago', icon: '👁️', color: '#ec4899', recentLogs: ['Pothole detected — severity 9, confidence 97%', 'Water burst confirmed — severity 10, confidence 99%', 'Garbage dump analyzed — severity 6, confidence 88%', 'Manhole hazard identified — critical risk'] },
  { id: 'a3', name: 'CategoryBot', role: 'Department Router', status: 'Active', lastAction: 'Routed 3 new reports to correct departments', actionsToday: 29, totalActions: 876, lastActionTime: '8 min ago', icon: '📂', color: '#06b6d4', recentLogs: ['Pothole → PWD', 'Water contamination → BWSSB (Priority)', 'Street lights → BESCOM', 'Garbage dump → BBMP'] },
  { id: 'a4', name: 'DuplicateBot', role: 'Deduplication Engine', status: 'Idle', lastAction: 'Merged 2 duplicate reports for Whitefield pipeline burst', actionsToday: 8, totalActions: 312, lastActionTime: '22 min ago', icon: '🔗', color: '#10b981', recentLogs: ['Merged NN-016 into NN-003 (same location)', 'Flagged NN-021 as potential duplicate', 'Distance check: 45m apart — same issue confirmed', '3 reports merged today'] },
  { id: 'a5', name: 'PriorityBot', role: 'Priority Scoring', status: 'Active', lastAction: 'Updated priority score for NN-003 to 98/100', actionsToday: 52, totalActions: 2134, lastActionTime: '1 min ago', icon: '⚡', color: '#f59e0b', recentLogs: ['NN-003: score updated 91→98 (high upvotes)', 'NN-014: score 96/100 (health risk)', 'NN-001: maintained 94/100', 'Recalculated 8 priority scores'] },
  { id: 'a6', name: 'PredictBot', role: 'Trend Predictor', status: 'Active', lastAction: 'Flagged Whitefield zone for pothole cluster prediction', actionsToday: 15, totalActions: 423, lastActionTime: '15 min ago', icon: '📊', color: '#3b82f6', recentLogs: ['Whitefield: pothole cluster alert', 'Hebbal: waterlogging risk — next rain', 'Koramangala: lights failure pattern detected', 'Ward 80: high complaint frequency this week'] },
  { id: 'a7', name: 'EscalationBot', role: 'SLA Enforcer', status: 'Active', lastAction: 'Triggered escalation for NN-007 — 8 days unresolved', actionsToday: 6, totalActions: 198, lastActionTime: '35 min ago', icon: '🚨', color: '#ef4444', recentLogs: ['Escalated NN-007 (8 days, health hazard)', 'Escalated NN-001 (7 days, critical)', 'Escalated NN-014 (6 days, water contamination)', 'Warning sent to BWSSB department head'] },
  { id: 'a8', name: 'ReportBot', role: 'Report Generator', status: 'Idle', lastAction: 'Generated Ward 80 monthly performance report', actionsToday: 3, totalActions: 87, lastActionTime: '2 hrs ago', icon: '📄', color: '#64748b', recentLogs: ['Monthly report: June 2026 ready', 'Ward 80 executive summary generated', 'BBMP performance dashboard updated', 'CSV export completed: 390 issues'] },
];

export const NOTIFICATIONS: Notification[] = [
  { id: 'n1', type: 'high_priority', title: 'Critical Issue Reported', message: 'Water pipeline burst in Whitefield — priority 98/100. Immediate action required.', time: '5 min ago', read: false, issueId: 'NN-003' },
  { id: 'n2', type: 'escalation', title: 'EscalationBot Alert', message: 'NN-007 escalated — sewage overflow in Malleshwaram unresolved for 8 days.', time: '35 min ago', read: false, issueId: 'NN-007' },
  { id: 'n3', type: 'escalation', title: 'Health Emergency Escalated', message: 'NN-014 — Water contamination in Yelahanka requires immediate BWSSB response.', time: '2 hrs ago', read: false, issueId: 'NN-014' },
  { id: 'n4', type: 'overdue', title: 'SLA Breach Warning', message: 'NN-001 pothole on Indiranagar 100ft Road has exceeded 7-day SLA.', time: '3 hrs ago', read: true, issueId: 'NN-001' },
  { id: 'n5', type: 'resolved', title: 'Issue Resolved ✅', message: 'NN-011 — Transformer malfunction in Rajajinagar has been resolved by BESCOM.', time: '4 hrs ago', read: true, issueId: 'NN-011' },
  { id: 'n6', type: 'new_report', title: 'New High Priority Report', message: 'Waterlogging at Silk Board Junction — 52 upvotes. Priority score: 80.', time: '5 hrs ago', read: true, issueId: 'NN-012' },
];

export const WARD_STATS: WardStat[] = [
  { ward: 'Ward 80', issues: 28, resolved: 16, avgScore: 72 },
  { ward: 'Ward 68', issues: 19, resolved: 14, avgScore: 61 },
  { ward: 'Ward 49', issues: 23, resolved: 10, avgScore: 79 },
  { ward: 'Ward 174', issues: 15, resolved: 12, avgScore: 55 },
  { ward: 'Ward 76', issues: 21, resolved: 9, avgScore: 74 },
  { ward: 'Ward 28', issues: 17, resolved: 7, avgScore: 68 },
];

export const TREND_DATA = [
  { month: 'Jan', reported: 145, resolved: 132, escalated: 8 },
  { month: 'Feb', reported: 162, resolved: 148, escalated: 11 },
  { month: 'Mar', reported: 188, resolved: 171, escalated: 14 },
  { month: 'Apr', reported: 201, resolved: 183, escalated: 12 },
  { month: 'May', reported: 234, resolved: 209, escalated: 18 },
  { month: 'Jun', reported: 390, resolved: 232, escalated: 29 },
];

export const CATEGORY_DATA = [
  { name: 'Pothole', value: 98, color: '#ef4444' },
  { name: 'Water', value: 87, color: '#3b82f6' },
  { name: 'Garbage', value: 76, color: '#10b981' },
  { name: 'Sewage', value: 64, color: '#8b5cf6' },
  { name: 'Lights', value: 63, color: '#f59e0b' },
  { name: 'Pedestrian', value: 52, color: '#ec4899' },
  { name: 'Other', value: 34, color: '#64748b' },
];

export const GEMINI_API_KEY = import.meta.env.VITE_GEMINI_API_KEY || '';
export const GOOGLE_MAPS_KEY = import.meta.env.VITE_GOOGLE_MAPS_KEY || 'AIzaSyDrctki_MLm7xlLQ7qLuQwKv_265oGW0aM';
