package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiAgentSystemTest {

    @Test
    fun testVisionBot() {
        // Base64 string must be long enough
        val validBase64 = "A".repeat(150)
        assertTrue(VisionBot.verifyImageContent(validBase64, "pothole"))
        assertFalse(VisionBot.verifyImageContent(null, "pothole"))
        assertFalse(VisionBot.verifyImageContent("", "pothole"))

        assertEquals(94, VisionBot.getConfidenceScore(validBase64))
        assertEquals(60, VisionBot.getConfidenceScore(null))
    }

    @Test
    fun testCategoryBot() {
        val water = CategoryBot.routeCategory("water")
        assertEquals("BWSSB", water.department)
        assertEquals(3, water.typicalDaysSla)

        val sewage = CategoryBot.routeCategory("sewage")
        assertEquals("BWSSB", sewage.department)
        assertEquals(2, sewage.typicalDaysSla)

        val pothole = CategoryBot.routeCategory("pothole")
        assertEquals("PWD", pothole.department)
        assertEquals(5, pothole.typicalDaysSla)

        val lights = CategoryBot.routeCategory("lights")
        assertEquals("BESCOM", lights.department)
        assertEquals(1, lights.typicalDaysSla)

        val garbage = CategoryBot.routeCategory("garbage")
        assertEquals("BBMP", garbage.department)
        assertEquals(1, garbage.typicalDaysSla)

        val gas = CategoryBot.routeCategory("gas")
        assertEquals("GAIL", gas.department)
        assertEquals(1, gas.typicalDaysSla)

        val unknown = CategoryBot.routeCategory("unknown_category")
        assertEquals("BBMP", unknown.department)
        assertEquals(7, unknown.typicalDaysSla)
    }

    @Test
    fun testDuplicateBot() {
        val now = System.currentTimeMillis()
        val baseLat = 12.9715987
        val baseLng = 77.5945627

        val existingIssues = listOf(
            Issue(
                id = "T-1",
                title = "Pothole Near Corner",
                category = "Pothole",
                ward = "Indiranagar (Ward 80)",
                locationName = "Main Road",
                latitude = baseLat + 0.001, // ~110m away
                longitude = baseLng,
                severity = 5,
                description = "Deep hole",
                timePosted = now,
                upvotes = 2,
                status = "Reported",
                isMyReport = false,
                assignedDept = "PWD"
            ),
            Issue(
                id = "T-2",
                title = "Water Leakage",
                category = "Water",
                ward = "Indiranagar (Ward 80)",
                locationName = "Side street",
                latitude = baseLat,
                longitude = baseLng + 0.01, // ~1.1km away
                severity = 3,
                description = "Leaking pipe",
                timePosted = now,
                upvotes = 1,
                status = "Reported",
                isMyReport = false,
                assignedDept = "BWSSB"
            )
        )

        // Duplicate checks
        val potholeDupes = DuplicateBot.findDuplicateCandidates("Pothole", baseLat, baseLng, existingIssues)
        assertEquals(1, potholeDupes.size)
        assertEquals("T-1", potholeDupes[0].id)

        val waterDupes = DuplicateBot.findDuplicateCandidates("Water", baseLat, baseLng, existingIssues)
        assertTrue(waterDupes.isEmpty()) // T-2 is > 500m

        assertFalse(DuplicateBot.isCluster(emptyList()))
        assertFalse(DuplicateBot.isCluster(listOf(existingIssues[0])))
        assertTrue(DuplicateBot.isCluster(listOf(existingIssues[0], existingIssues[0])))
    }

    @Test
    fun testPriorityBot() {
        val now = System.currentTimeMillis()
        val score = PriorityBot.calculatePriorityScore(severity = 7, upvotes = 4, timePosted = now, currentTimeMs = now)
        // Score = (7 * 8) + (4 * 2) + (0 * 3) = 56 + 8 = 64
        assertEquals(64, score)
    }

    @Test
    fun testPredictBot() {
        val now = System.currentTimeMillis()
        val baseLat = 12.9715987
        val baseLng = 77.5945627

        val singleIssue = listOf(
            Issue("T-1", "Pothole A", "Pothole", "Ward 80", "Loc", baseLat + 0.001, baseLng, 5, "Desc", now, 1, "Reported", false, "PWD")
        )

        val stableTrend = PredictBot.analyzeFrequency("Pothole", baseLat, baseLng, singleIssue)
        assertTrue(stableTrend.contains("STABLE PREDICTION"))

        // Add 5 issues within 1km
        val clusterIssues = List(5) { i ->
            Issue("T-$i", "Pothole $i", "Pothole", "Ward 80", "Loc", baseLat + 0.002, baseLng, 5, "Desc", now, 1, "Reported", false, "PWD")
        }
        val criticalTrend = PredictBot.analyzeFrequency("Pothole", baseLat, baseLng, clusterIssues)
        assertTrue(criticalTrend.contains("CRITICAL DENSITY TREND"))
    }

    @Test
    fun testEscalationBot() {
        val now = System.currentTimeMillis()
        val baseIssue = Issue("T-1", "Pothole", "Pothole", "Ward 80", "Loc", 12.97, 77.59, 5, "Desc", now, 1, "Reported", false, "PWD", slaStatus = "SLA OK")

        // Under 7 days -> No escalation
        val normAction = EscalationBot.checkSlaStatus(baseIssue, now + (3L * 24 * 60 * 60 * 1000L))
        assertFalse(normAction.shouldEscalate)

        // 7 days -> Soft escalation (severity + 1)
        val softAction = EscalationBot.checkSlaStatus(baseIssue, now + (8L * 24 * 60 * 60 * 1000L))
        assertTrue(softAction.shouldEscalate)
        assertEquals("PENDING_ESCALATION", softAction.newSlaStatus)
        assertEquals(6, softAction.newSeverity)

        // 14 days -> Urgent escalation (severity + 2)
        val urgentAction = EscalationBot.checkSlaStatus(baseIssue, now + (15L * 24 * 60 * 60 * 1000L))
        assertTrue(urgentAction.shouldEscalate)
        assertEquals("SLA BREACHED", urgentAction.newSlaStatus)
        assertEquals(7, urgentAction.newSeverity)

        // 30 days -> Critical escalation (severity = 10)
        val critAction = EscalationBot.checkSlaStatus(baseIssue, now + (31L * 24 * 60 * 60 * 1000L))
        assertTrue(critAction.shouldEscalate)
        assertEquals("SLA BREACHED", critAction.newSlaStatus)
        assertEquals(10, critAction.newSeverity)
    }

    @Test
    fun testReportBot() {
        val now = System.currentTimeMillis()
        val issues = listOf(
            Issue("T-1", "Pothole", "Pothole", "Indiranagar (Ward 80)", "Loc", 12.97, 77.59, 5, "Desc", now, 1, "Reported", false, "PWD"),
            Issue("T-2", "Garbage", "Garbage", "Indiranagar (Ward 80)", "Loc", 12.97, 77.59, 3, "Desc", now, 1, "Resolved", false, "BBMP")
        )
        val summary = ReportBot.generateWardSummary("Indiranagar", issues)
        assertTrue(summary.contains("Active issues: 1"))
        assertTrue(summary.contains("Resolved issues: 1"))
    }

    @Test
    fun testOrchestratorBotDecisionFlow() {
        val now = System.currentTimeMillis()
        val baseLat = 12.9715987
        val baseLng = 77.5945627
        val orchestrator = OrchestratorBot()

        // 1. Isolated standard creation
        val decisionCreate = orchestrator.processNewReport("Pothole", 5, baseLat, baseLng, emptyList())
        assertEquals("CREATE", decisionCreate.action)
        assertEquals("PWD", decisionCreate.notifyDepartment)

        // 2. Duplicate Merge
        val existingIssue = Issue("T-1", "Pothole", "Pothole", "Ward 80", "Loc", baseLat + 0.001, baseLng, 5, "Desc", now, 1, "Reported", false, "PWD", slaStatus = "SLA OK")
        val decisionMerge = orchestrator.processNewReport("Pothole", 5, baseLat, baseLng, listOf(existingIssue))
        assertEquals("MERGE", decisionMerge.action)
        assertEquals("T-1", decisionMerge.targetIssueId)

        // 3. Cluster Escalation (2 existing issues + 1 new)
        val secondIssue = Issue("T-2", "Pothole B", "Pothole", "Ward 80", "Loc", baseLat - 0.001, baseLng, 5, "Desc", now, 1, "Reported", false, "PWD", slaStatus = "SLA OK")
        val decisionCluster = orchestrator.processNewReport("Pothole", 5, baseLat, baseLng, listOf(existingIssue, secondIssue))
        assertEquals("CLUSTER_ESCALATE", decisionCluster.action)

        // 4. Emergency Escalation (Gas category)
        val decisionEmergency = orchestrator.processNewReport("Gas", 9, baseLat, baseLng, emptyList())
        assertEquals("CLUSTER_ESCALATE", decisionEmergency.action)
        assertEquals("GAIL Emergency", decisionEmergency.notifyDepartment)
        assertEquals(UrgencyLevel.EMERGENCY, decisionEmergency.urgencyLevel)
    }
    @Test
    fun testCitizenAssistantPolicyTicketSpecificAnswer() {
        val now = System.currentTimeMillis()
        val issues = listOf(
            Issue("IDS-777", "Broken streetlight near park", "Lights", "Ward 80", "Green Park", 12.97, 77.59, 8, "Dark stretch near park gate", now, 4, "In Progress", false, "BESCOM")
        )

        val answer = CivicAssistantPolicy.fallback("status of IDS-777", issues)

        assertTrue(answer.contains("IDS-777"))
        assertTrue(answer.contains("In Progress"))
        assertTrue(answer.contains("BESCOM"))
        assertFalse(answer.contains("Tell me a bit more"))
    }

    @Test
    fun testCitizenAssistantPolicyRedirectsOutOfScopeQuestions() {
        val answer = CivicAssistantPolicy.fallback("write my school essay", emptyList())

        assertTrue(answer.contains("NagarNetra civic questions only"))
        assertTrue(answer.length < 180)
    }

    @Test
    fun testCitizenAssistantPolicyReplacesGenericAiResponse() {
        val cleaned = CivicAssistantPolicy.enforceScope(
            userQuery = "why",
            answer = "Usually the reason comes from cause, context, and impact. If you share a little more detail, I can make the answer more specific.",
            allIssues = emptyList()
        )

        assertTrue(cleaned.contains("NagarNetra civic questions only"))
        assertFalse(cleaned.contains("cause, context, and impact"))
    }


}
