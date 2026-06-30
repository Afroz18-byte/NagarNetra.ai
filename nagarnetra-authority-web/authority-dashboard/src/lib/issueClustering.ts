import type { Department, Issue, IssueCategory, RiskLevel } from '../types';

export interface IssueCluster {
  id: string;
  issues: Issue[];
  count: number;
  latitude: number;
  longitude: number;
  category: IssueCategory;
  locationName: string;
  objectId?: string;
  priorityScore: number;
  riskLevel: RiskLevel;
}

const CATEGORY_DEPT: Record<IssueCategory, Department> = {
  Pothole: 'PWD',
  Water: 'BWSSB',
  Lights: 'BESCOM',
  Garbage: 'BBMP',
  Sewage: 'BWSSB',
  Pedestrian: 'PWD',
  Other: 'BBMP',
};

export function inferCategory(text: string): IssueCategory {
  const t = text.toLowerCase();
  if (/\b(pothole|road|cave|divider|asphalt)\b/.test(t)) return 'Pothole';
  if (/\b(water|pipeline|leak|flood|supply|contamination)\b/.test(t)) return 'Water';
  if (/\b(light|streetlight|electric|power|blackout|transformer)\b/.test(t)) return 'Lights';
  if (/\b(garbage|trash|waste|dump|bin)\b/.test(t)) return 'Garbage';
  if (/\b(sewage|sewer|drain|manhole|storm drain)\b/.test(t)) return 'Sewage';
  if (/\b(footpath|sidewalk|pedestrian|crossing|tile)\b/.test(t)) return 'Pedestrian';
  return 'Other';
}

export function departmentForCategory(category: IssueCategory): Department {
  return CATEGORY_DEPT[category] || 'BBMP';
}

export function extractObjectId(text: string): string | undefined {
  const match = text.match(/\b(?:pc|pole|light|manhole|mh|bin|pipe|meter|pump|transformer|tf|road)\s*[-#:]?\s*([a-z0-9]{1,8})\b/i);
  if (!match) return undefined;
  return match[0].replace(/\s+/g, '').toUpperCase();
}

export function calculatePriority(issue: Pick<Issue, 'severity' | 'upvotes' | 'duplicateReports'>): number {
  const duplicateBoost = Math.min(24, Math.max(0, (issue.duplicateReports || 1) - 1) * 6);
  const upvoteBoost = Math.min(22, (issue.upvotes || 0) * 2);
  return Math.min(100, Math.round((issue.severity || 5) * 7 + upvoteBoost + duplicateBoost));
}

export function riskForScore(score: number): RiskLevel {
  if (score >= 90) return 'Critical';
  if (score >= 72) return 'High';
  if (score >= 45) return 'Medium';
  return 'Low';
}

export function distanceMeters(a: Pick<Issue, 'latitude' | 'longitude'>, b: Pick<Issue, 'latitude' | 'longitude'>): number {
  const earth = 6371000;
  const dLat = ((b.latitude - a.latitude) * Math.PI) / 180;
  const dLng = ((b.longitude - a.longitude) * Math.PI) / 180;
  const lat1 = (a.latitude * Math.PI) / 180;
  const lat2 = (b.latitude * Math.PI) / 180;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * earth * Math.asin(Math.sqrt(h));
}

function sameObject(a: Issue, b: Issue) {
  return Boolean(a.objectId && b.objectId && a.objectId.toUpperCase() === b.objectId.toUpperCase());
}

function samePlace(a: Issue, b: Issue) {
  const locationA = (a.locationName || '').toLowerCase().replace(/[^a-z0-9]/g, '');
  const locationB = (b.locationName || '').toLowerCase().replace(/[^a-z0-9]/g, '');
  return locationA.length > 5 && locationA === locationB;
}

export function findDuplicateIssue(newIssue: Issue, issues: Issue[]): Issue | undefined {
  return issues.find(existing => {
    if (existing.status === 'Resolved') return false;
    if (existing.category !== newIssue.category && !sameObject(existing, newIssue)) return false;
    const closeEnough = distanceMeters(existing, newIssue) <= (sameObject(existing, newIssue) ? 250 : 120);
    return closeEnough || (sameObject(existing, newIssue) && samePlace(existing, newIssue));
  });
}

export function buildIssueClusters(issues: Issue[], radiusMeters = 260): IssueCluster[] {
  const clusters: IssueCluster[] = [];
  issues.forEach(issue => {
    const existing = clusters.find(cluster => {
      const anchor = { latitude: cluster.latitude, longitude: cluster.longitude };
      const sameCategory = cluster.category === issue.category;
      const objectMatch = cluster.objectId && issue.objectId && cluster.objectId.toUpperCase() === issue.objectId.toUpperCase();
      return (sameCategory || objectMatch) && distanceMeters(anchor, issue) <= radiusMeters;
    });

    if (existing) {
      existing.issues.push(issue);
      existing.count += Math.max(1, issue.duplicateReports || 1);
      existing.latitude = existing.issues.reduce((sum, i) => sum + i.latitude, 0) / existing.issues.length;
      existing.longitude = existing.issues.reduce((sum, i) => sum + i.longitude, 0) / existing.issues.length;
      existing.priorityScore = Math.max(existing.priorityScore, calculatePriority(issue));
      existing.riskLevel = riskForScore(existing.priorityScore);
    } else {
      const priorityScore = calculatePriority(issue);
      clusters.push({
        id: `${issue.category}-${issue.objectId || issue.id}`,
        issues: [issue],
        count: Math.max(1, issue.duplicateReports || 1),
        latitude: issue.latitude,
        longitude: issue.longitude,
        category: issue.category,
        locationName: issue.locationName,
        objectId: issue.objectId,
        priorityScore,
        riskLevel: riskForScore(priorityScore),
      });
    }
  });

  return clusters.filter(cluster => cluster.count > 1 || cluster.issues.length > 1);
}
