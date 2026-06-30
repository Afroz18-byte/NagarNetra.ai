/**
 * OrchestratorBot Agent
 * 
 * Responsible for parsing output from Gemini Image analysis, determining 
 * whether a newly reported issue is a duplicate/proximity-merge candidate, 
 * or if it requires immediate escalation based on severity and category.
 */

export interface AnalysisOutput {
  category: string;
  severity: number;
  description: string;
}

export interface ExistingIssue {
  id: string;
  title: string;
  category: string;
  latitude: number;
  longitude: number;
  severity: number;
  status: string;
  slaStatus: string;
}

export interface OrchestrationDecision {
  action: 'CREATE' | 'MERGE' | 'ESCALATE';
  targetIssueId?: string;
  reason: string;
  recommendedSlaStatus: string;
}

/**
 * Calculates distance in meters between two geocoordinates using the Haversine formula.
 */
function calculateDistanceInMeters(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371e3; // Earth's radius in meters
  const phi1 = (lat1 * Math.PI) / 180;
  const phi2 = (lat2 * Math.PI) / 180;
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

export class OrchestratorBot {
  private proximityThresholdMeters: number = 200; // 200 meters proximity for duplicate matching

  /**
   * Processes the output of the image analyzer to determine the lifecycle path of the new issue report.
   */
  public processNewReport(
    analysis: AnalysisOutput,
    latitude: number,
    longitude: number,
    existingIssues: ExistingIssue[]
  ): OrchestrationDecision {
    // 1. Proximity Merging Check
    // If there is an active issue in the same category within the proximity threshold, we merge them.
    const duplicateCandidate = existingIssues.find((issue) => {
      if (issue.status === 'Resolved') return false;
      if (issue.category.toLowerCase() !== analysis.category.toLowerCase()) return false;

      const distance = calculateDistanceInMeters(
        latitude,
        longitude,
        issue.latitude,
        issue.longitude
      );
      return distance <= this.proximityThresholdMeters;
    });

    if (duplicateCandidate) {
      return {
        action: 'MERGE',
        targetIssueId: duplicateCandidate.id,
        reason: `Proximity match found within ${this.proximityThresholdMeters}m (Ticket ${duplicateCandidate.id} in ${duplicateCandidate.category}). Merging upvotes and description.`,
        recommendedSlaStatus: duplicateCandidate.slaStatus
      };
    }

    // 2. Escalation Check
    // Critical safety situations (severity >= 8, or heavy categories) trigger immediate PENDING_ESCALATION
    const isCriticalCategory = ['Sewage', 'Water', 'Roads', 'Gas'].includes(analysis.category);
    if (analysis.severity >= 8 || (analysis.severity >= 6 && isCriticalCategory)) {
      return {
        action: 'ESCALATE',
        reason: `High severity warning (${analysis.severity}/10) for critical municipal category '${analysis.category}'. Escalation requested.`,
        recommendedSlaStatus: 'PENDING_ESCALATION'
      };
    }

    // 3. Keep standard creation routing
    return {
      action: 'CREATE',
      reason: `No nearby active issues found for category '${analysis.category}'. Default SLA track applied.`,
      recommendedSlaStatus: 'SLA OK'
    };
  }
}
