package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "issues")
data class Issue(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val ward: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Int, // 1 to 10
    val description: String,
    val timePosted: Long,
    val upvotes: Int,
    val status: String, // "Reported", "Verified", "In Progress", "Resolved"
    val isMyReport: Boolean,
    val assignedDept: String, // "PWD", "BESCOM", "BWSSB", "BBMP"
    val photoBase64: String? = null,
    val resolutionPhotoBase64: String? = null,
    val resolutionNotes: String? = null,
    val slaStatus: String = "SLA OK", // "SLA OK", "SLA BREACHED", "PENDING_ESCALATION"
    val requiresReview: Boolean = false,
    val verificationReasons: String = "",
    val objectId: String? = null,
    val duplicateReports: Int = 1,
    val clusteredIssueIdsCsv: String = "",
    val source: String = "Mobile App",
    val voiceTranscript: String = ""
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey val id: String,
    val email: String,
    val name: String = "Rahul",
    val ward: String = "Indiranagar (Ward 80)",
    val trustScore: Int = 88,
    val points: Int = 150,
    val level: Int = 4,
    val rank: Int = 14,
    val badgesCount: Int = 5,
    val reportsCount: Int = 3,
    val authMethod: String = "email",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "authority_logs")
data class AuthorityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val isBotAction: Boolean, // true = OrchestratorBot action, false = other action
    val message: String,
    val priority: String // "Low", "Medium", "High", "Critical"
)

@Entity(tableName = "ai_decision_logs")
data class AiDecisionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val agentName: String, // e.g. "VisionBot", "CategoryBot", "DuplicateBot", "PriorityBot", "PredictBot", "EscalationBot", "ReportBot"
    val issueId: String?,
    val decision: String,
    val detail: String
)

@Entity(tableName = "email_logs")
data class EmailLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val issueId: String?,
    val recipient: String,
    val subject: String,
    val body: String,
    val triggerType: String, // "HIGH_VOTES" | "SEVEN_DAY" | "FOURTEEN_DAY" | "THIRTY_DAY" | "WEEKLY_SUMMARY" | "MONTHLY_REPORT"
    val isSimulated: Boolean,
    val status: String
)

@Entity(tableName = "ward_reports")
data class WardReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val reportType: String, // "WEEKLY" | "MONTHLY"
    val reportText: String,
    val ward: String
)

@Entity(tableName = "community_messages")
data class CommunityChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roomId: String, // e.g. "ward_113", "nearby", "emergency", "issue_IDS-181"
    val senderId: String,
    val senderName: String,
    val senderTrustScore: Int,
    val senderWard: String,
    val text: String,
    val timestamp: Long,
    val photoBase64: String? = null,
    val parentMessageId: Int? = null, // for threading/replies
    val reactionsJson: String = "{}", // JSON Map for emoji reactions e.g. {"👍": 2}
    val upvotes: Int = 0,
    val isSystem: Boolean = false,
    val isHiddenByAi: Boolean = false
)


