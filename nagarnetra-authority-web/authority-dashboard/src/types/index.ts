export type IssueStatus = 'Open' | 'In Progress' | 'Resolved' | 'Escalated';
export type IssueCategory = 'Pothole' | 'Water' | 'Lights' | 'Garbage' | 'Sewage' | 'Pedestrian' | 'Other';
export type Department = 'PWD' | 'BWSSB' | 'BESCOM' | 'BBMP';
export type RiskLevel = 'Low' | 'Medium' | 'High' | 'Critical';
export type UserRole = 'Super Admin' | 'Department Admin' | 'Field Officer';

export interface Issue {
  id: string;
  title: string;
  description: string;
  category: IssueCategory;
  status: IssueStatus;
  severity: number;
  riskLevel: RiskLevel;
  assignedDept: Department;
  locationName: string;
  latitude: number;
  longitude: number;
  upvotes: number;
  timePosted: string;
  ward: string;
  slaStatus: string;
  priorityScore: number;
  assignedOfficer?: string;
  resolutionNotes?: string;
  daysOpen: number;
  reporterName?: string;
  escalationReason?: string;
  aiConfidence?: number;
  photoBase64?: string;
  resolutionPhotoBase64?: string;
  objectId?: string;
  duplicateReports?: number;
  clusteredIssueIds?: string[];
  voiceTranscript?: string;
  source?: 'Manual' | 'Voice Agent' | 'Mobile App' | 'Web';
}

export interface DepartmentStat {
  name: Department;
  displayName: string;
  totalIssues: number;
  resolved: number;
  pending: number;
  escalated: number;
  avgResolutionDays: number;
  head: string;
  color: string;
}

export interface AgentActivity {
  id: string;
  name: string;
  role: string;
  status: 'Active' | 'Idle' | 'Processing';
  lastAction: string;
  actionsToday: number;
  totalActions: number;
  lastActionTime: string;
  icon: string;
  color: string;
  recentLogs: string[];
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  department?: Department;
  avatar: string;
}

export interface Notification {
  id: string;
  type: 'high_priority' | 'escalation' | 'overdue' | 'resolved' | 'new_report';
  title: string;
  message: string;
  time: string;
  read: boolean;
  issueId?: string;
}

export interface WardStat {
  ward: string;
  issues: number;
  resolved: number;
  avgScore: number;
}
