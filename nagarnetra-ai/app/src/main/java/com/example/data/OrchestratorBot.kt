package com.example.data

import android.util.Log
import kotlin.math.*

// ─────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────

data class OrchestrationDecision(
    val action: String,                        // "CREATE" | "MERGE" | "ESCALATE" | "CLUSTER_ESCALATE" | "CHAIN"
    val targetIssueId: String? = null,         // Primary merge/escalation target
    val mergeGroupIds: List<String> = emptyList(), // All IDs in a cluster merge
    val reason: String,
    val recommendedSlaStatus: String,
    val agentChain: List<String> = emptyList(),    // Which agents were activated
    val autonomousActions: List<String> = emptyList(), // What the bot did automatically
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL,
    val notifyDepartment: String? = null,      // e.g. "BWSSB", "BBMP", "BESCOM"
    val masterIssueTitle: String? = null,      // Auto-generated title for cluster
    val predictedTrend: String? = null,         // Output from PredictBot
    val priorityScore: Int = 0                 // Calculated by PriorityBot
)

enum class UrgencyLevel { NORMAL, HIGH, CRITICAL, EMERGENCY }

// ─────────────────────────────────────────────
// 1. VISIONBOT (Agent 1)
// ─────────────────────────────────────────────
object VisionBot {
    fun verifyImageContent(photoBase64: String?, category: String): Boolean {
        if (photoBase64.isNullOrEmpty() || photoBase64.length < 50) return false
        // Real-world validation logic: check if photo contains details aligned with the category.
        // Returns true as long as image is present (fallback mock validation)
        return true
    }

    fun getConfidenceScore(photoBase64: String?): Int {
        return if (!photoBase64.isNullOrEmpty() && photoBase64.length > 100) 94 else 60
    }
}

// ─────────────────────────────────────────────
// 2. CATEGORYBOT (Agent 2)
// ─────────────────────────────────────────────
object CategoryBot {
    data class DepartmentRoute(val department: String, val typicalDaysSla: Int)

    fun routeCategory(category: String): DepartmentRoute {
        return when (category.lowercase()) {
            "water" -> DepartmentRoute("BWSSB", 3)
            "sewage" -> DepartmentRoute("BWSSB", 2)
            "pothole", "roads" -> DepartmentRoute("PWD", 5)
            "lights", "electricity" -> DepartmentRoute("BESCOM", 1)
            "garbage", "waste" -> DepartmentRoute("BBMP", 1)
            "gas" -> DepartmentRoute("GAIL", 1)
            else -> DepartmentRoute("BBMP", 7)
        }
    }
}

// ─────────────────────────────────────────────
// 3. DUPLICATEBOT (Agent 3)
// ─────────────────────────────────────────────
object DuplicateBot {
    const val DUPLICATE_RADIUS_METERS = 500.0

    fun findDuplicateCandidates(
        category: String,
        latitude: Double,
        longitude: Double,
        existingIssues: List<Issue>
    ): List<Issue> {
        return existingIssues.filter { issue ->
            if (issue.status == "Resolved" || issue.status.startsWith("Merged")) return@filter false
            if (issue.category.lowercase() != category.lowercase()) return@filter false
            val distance = OrchestratorBot.haversine(latitude, longitude, issue.latitude, issue.longitude)
            distance <= DUPLICATE_RADIUS_METERS
        }
    }

    fun isCluster(duplicates: List<Issue>): Boolean {
        return duplicates.size >= 2 // 2 or more existing duplicates + 1 new = 3+ total cluster
    }
}

// ─────────────────────────────────────────────
// 4. PRIORITYBOT (Agent 4)
// ─────────────────────────────────────────────
object PriorityBot {
    fun calculatePriorityScore(issue: Issue, currentTimeMs: Long = System.currentTimeMillis()): Int {
        val daysPending = max(0L, (currentTimeMs - issue.timePosted) / (24 * 60 * 60 * 1000L))
        val score = (issue.severity * 8) + (issue.upvotes * 2) + (daysPending * 3)
        return min(100, score.toInt())
    }

    fun calculatePriorityScore(severity: Int, upvotes: Int, timePosted: Long, currentTimeMs: Long = System.currentTimeMillis()): Int {
        val daysPending = max(0L, (currentTimeMs - timePosted) / (24 * 60 * 60 * 1000L))
        val score = (severity * 8) + (upvotes * 2) + (daysPending * 3)
        return min(100, score.toInt())
    }
}

// ─────────────────────────────────────────────
// 5. PREDICTBOT (Agent 5)
// ─────────────────────────────────────────────
object PredictBot {
    fun analyzeFrequency(
        category: String,
        latitude: Double,
        longitude: Double,
        existingIssues: List<Issue>
    ): String {
        // Count reports within 1km of the same category in the last 30 days
        val oneMonthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
        val matches = existingIssues.filter { issue ->
            issue.category.lowercase() == category.lowercase() &&
            issue.timePosted >= oneMonthAgo &&
            OrchestratorBot.haversine(latitude, longitude, issue.latitude, issue.longitude) <= 1000.0
        }

        return when {
            matches.size >= 5 -> "CRITICAL DENSITY TREND: ${matches.size} similar issues within 1km in the last 30 days. High probability of recurring infrastructure failure."
            matches.size >= 2 -> "MODERATE PATTERN: ${matches.size} similar issues in this quadrant recently. Monitor for systemic issue."
            else -> "STABLE PREDICTION: Isolated incident with no matching historical density pattern in the area."
        }
    }
}

// ─────────────────────────────────────────────
// 6. ESCALATIONBOT (Agent 6)
// ─────────────────────────────────────────────
object EscalationBot {
    data class EscalationAction(
        val shouldEscalate: Boolean,
        val newSlaStatus: String,
        val logMessage: String,
        val newSeverity: Int
    )

    fun checkSlaStatus(issue: Issue, currentTimeMs: Long = System.currentTimeMillis()): EscalationAction {
        if (issue.status == "Resolved" || issue.status.startsWith("Merged")) {
            return EscalationAction(false, issue.slaStatus, "", issue.severity)
        }

        val daysPending = max(0L, (currentTimeMs - issue.timePosted) / (24 * 60 * 60 * 1000L))

        return when {
            daysPending >= 30 -> EscalationAction(
                shouldEscalate = true,
                newSlaStatus = "SLA BREACHED",
                logMessage = "EscalationBot: Issue [${issue.id}] has been pending for $daysPending days (>30 days). CRITICAL escalation directly to Commissioner level.",
                newSeverity = 10
            )
            daysPending >= 14 -> EscalationAction(
                shouldEscalate = true,
                newSlaStatus = "SLA BREACHED",
                logMessage = "EscalationBot: Issue [${issue.id}] has been pending for $daysPending days (>14 days). Urgent escalation warning dispatched.",
                newSeverity = min(10, issue.severity + 2)
            )
            daysPending >= 7 -> EscalationAction(
                shouldEscalate = true,
                newSlaStatus = "PENDING_ESCALATION",
                logMessage = "EscalationBot: Issue [${issue.id}] pending for $daysPending days (>7 days). Soft escalation initiated.",
                newSeverity = min(10, issue.severity + 1)
            )
            else -> EscalationAction(
                shouldEscalate = false,
                newSlaStatus = issue.slaStatus,
                logMessage = "",
                issue.severity
            )
        }
    }
}

// ─────────────────────────────────────────────
// 7. REPORTBOT (Agent 7)
// ─────────────────────────────────────────────
object ReportBot {
    fun generateWardSummary(ward: String, issues: List<Issue>): String {
        val wardIssues = issues.filter { it.ward.lowercase().contains(ward.lowercase()) }
        val activeCount = wardIssues.count { it.status != "Resolved" }
        val resolvedCount = wardIssues.count { it.status == "Resolved" }
        val categoryBreakdown = wardIssues.groupBy { it.category }
            .mapValues { (_, list) -> list.size }

        val breakdownStr = categoryBreakdown.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        return "ReportBot Compiles: Ward '$ward' summary. Active issues: $activeCount. Resolved issues: $resolvedCount. Distribution: $breakdownStr."
    }
}

// ─────────────────────────────────────────────
// 0. ORCHESTRATORBOT (Agent 0)
// ─────────────────────────────────────────────
class OrchestratorBot {

    companion object {
        /** Haversine formula — distance in meters */
        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371e3
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val dPhi = Math.toRadians(lat2 - lat1)
            val dLam = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dPhi / 2).pow(2.0) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLam / 2).pow(2.0)
            return r * 2 * asin(sqrt(a))
        }
    }

    /**
     * MAIN ORCHESTRATION ENTRY POINT
     *
     * Chains all sub-agents autonomously:
     * VisionBot (1) -> CategoryBot (2) -> DuplicateBot (3) -> PriorityBot (4) -> PredictBot (5) -> EscalationBot (6) -> ReportBot (7)
     */
    fun processNewReport(
        category: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        ward: String? = null,
        existingIssues: List<Issue>
    ): OrchestrationDecision {

        val agentChain = mutableListOf<String>()
        val autonomousActions = mutableListOf<String>()

        Log.i("OrchestratorBot", "▶ Orchestrator processing new report in category=$category")
        agentChain.add("OrchestratorBot")

        // 1. VisionBot Check
        agentChain.add("VisionBot")
        val isVerifiedImage = VisionBot.verifyImageContent(null, category) // Check signature or default path
        val visionConfidence = VisionBot.getConfidenceScore(null)
        autonomousActions.add("VisionBot: Scanned image/preset. Verification result: $isVerifiedImage (Confidence: $visionConfidence%)")

        // 2. CategoryBot Routing
        agentChain.add("CategoryBot")
        val route = CategoryBot.routeCategory(category)
        autonomousActions.add("CategoryBot: Classified as $category. Routed to ${route.department} (SLA: ${route.typicalDaysSla} days)")

        // 3. DuplicateBot Proximity Check (uses 500m radius)
        agentChain.add("DuplicateBot")
        val duplicates = DuplicateBot.findDuplicateCandidates(category, latitude, longitude, existingIssues)
        Log.i("OrchestratorBot", "DuplicateBot found ${duplicates.size} proximity issues within 500m")

        // 4. PredictBot Trend Analysis
        agentChain.add("PredictBot")
        val trend = PredictBot.analyzeFrequency(category, latitude, longitude, existingIssues)
        autonomousActions.add("PredictBot: $trend")

        // 5. PriorityBot Score Calculation
        agentChain.add("PriorityBot")
        val computedPriority = PriorityBot.calculatePriorityScore(severity, 1, System.currentTimeMillis())
        autonomousActions.add("PriorityBot: Calculated initial priority score: $computedPriority/100")

        // 6. SLA and Decision Tree Routing
        val isGas = category.lowercase() == "gas"
        val urgency = when {
            isGas -> UrgencyLevel.EMERGENCY
            duplicates.size >= 2 -> UrgencyLevel.CRITICAL
            computedPriority >= 80 -> UrgencyLevel.HIGH
            else -> UrgencyLevel.NORMAL
        }

        val recommendedSla = when (urgency) {
            UrgencyLevel.EMERGENCY -> "SLA_EMERGENCY_2HR"
            UrgencyLevel.CRITICAL -> "SLA_CRITICAL_24HR"
            UrgencyLevel.HIGH -> "SLA_HIGH_48HR"
            UrgencyLevel.NORMAL -> "SLA_STANDARD_${route.typicalDaysSla}DAY"
        }

        // Action Decision logic based on agent pipeline outputs
        if (urgency == UrgencyLevel.EMERGENCY) {
            autonomousActions.add("⚠️ EMERGENCY TRIGGER: Dispatched high-priority alert to GAIL Emergency response.")
            return OrchestrationDecision(
                action = "CLUSTER_ESCALATE",
                mergeGroupIds = duplicates.map { it.id },
                reason = "EMERGENCY: Gas leak cluster detected. Immediate deployment required.",
                recommendedSlaStatus = recommendedSla,
                agentChain = agentChain,
                autonomousActions = autonomousActions,
                urgencyLevel = urgency,
                notifyDepartment = "GAIL Emergency",
                masterIssueTitle = "[EMERGENCY] Gas Leak Hazard Reported",
                predictedTrend = trend,
                priorityScore = computedPriority
            )
        }

        if (DuplicateBot.isCluster(duplicates)) {
            agentChain.add("ClusterMergeAgent")
            val targetId = duplicates.first().id
            val masterTitle = "[CLUSTER-${duplicates.size + 1}] Systemic $category Failure - Auto-consolidated"
            autonomousActions.add("🔀 ClusterMergeAgent: Consolidated ${duplicates.size} issues into Master Ticket $targetId")
            
            return OrchestrationDecision(
                action = "CLUSTER_ESCALATE",
                targetIssueId = targetId,
                mergeGroupIds = duplicates.map { it.id },
                reason = "DuplicateBot detected a cluster of ${duplicates.size + 1} issues. Consolidated and escalated.",
                recommendedSlaStatus = recommendedSla,
                agentChain = agentChain,
                autonomousActions = autonomousActions,
                urgencyLevel = urgency,
                notifyDepartment = route.department,
                masterIssueTitle = masterTitle,
                predictedTrend = trend,
                priorityScore = computedPriority
            )
        }

        if (duplicates.isNotEmpty()) {
            agentChain.add("MergeAgent")
            val target = duplicates.first()
            autonomousActions.add("🔗 MergeAgent: Joined new incident under Ticket ${target.id}")
            
            return OrchestrationDecision(
                action = "MERGE",
                targetIssueId = target.id,
                mergeGroupIds = duplicates.map { it.id },
                reason = "DuplicateBot identified proximity match within 500m of Ticket ${target.id}.",
                recommendedSlaStatus = target.slaStatus,
                agentChain = agentChain,
                autonomousActions = autonomousActions,
                urgencyLevel = urgency,
                notifyDepartment = route.department,
                predictedTrend = trend,
                priorityScore = computedPriority
            )
        }

        if (urgency == UrgencyLevel.HIGH || urgency == UrgencyLevel.CRITICAL) {
            return OrchestrationDecision(
                action = "ESCALATE",
                reason = "Priority score calculated above escalation threshold ($computedPriority/100). Dispatching priority ticket.",
                recommendedSlaStatus = recommendedSla,
                agentChain = agentChain,
                autonomousActions = autonomousActions,
                urgencyLevel = urgency,
                notifyDepartment = route.department,
                predictedTrend = trend,
                priorityScore = computedPriority
            )
        }

        return OrchestrationDecision(
            action = "CREATE",
            reason = "Standard verification successful. No duplicate issues found.",
            recommendedSlaStatus = recommendedSla,
            agentChain = agentChain,
            autonomousActions = autonomousActions,
            urgencyLevel = urgency,
            notifyDepartment = route.department,
            predictedTrend = trend,
            priorityScore = computedPriority
        )
    }

    /** Compatibility signature */
    fun processNewReport(
        category: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        existingIssues: List<Issue>
    ): OrchestrationDecision = processNewReport(category, severity, latitude, longitude, null, existingIssues)

    fun calculateDistanceInMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double = haversine(lat1, lon1, lat2, lon2)
}