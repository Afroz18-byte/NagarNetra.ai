package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ─── NagarNetra Civic Design System ──────────────────────────────────────────
// Primary:   Civic Navy     — trust, authority, government credibility
// Secondary: Signal Teal    — modern civic-tech accent
// Tertiary:  Alert Amber    — status escalations and warnings
// Error:     Danger Red     — severe issue severity signal
// Success:   Resolution Green — confirmed resolved state
// ─────────────────────────────────────────────────────────────────────────────

// ── Light scheme ──────────────────────────────────────────────────────────────
val CivicNavy          = Color(0xFF0D5C4A)   // primary
val CivicNavyContainer = Color(0xFFDCE4F0)   // primaryContainer
val OnCivicNavy        = Color(0xFFFFFFFF)   // onPrimary
val OnCivicNavyContainer = Color(0xFF0F2340) // onPrimaryContainer

val SignalTeal          = Color(0xFF0EA5A4)  // secondary
val SignalTealContainer = Color(0xFFCCF0EF)  // secondaryContainer
val OnSignalTeal        = Color(0xFFFFFFFF)  // onSecondary
val OnSignalTealContainer = Color(0xFF003737)// onSecondaryContainer

val AlertAmber          = Color(0xFFF59E0B)  // tertiary
val AlertAmberContainer = Color(0xFFFFF3CD)  // tertiaryContainer
val OnAlertAmber        = Color(0xFF1C1000)  // onTertiary
val OnAlertAmberContainer = Color(0xFF4A2D00)// onTertiaryContainer

val DangerRed           = Color(0xFFDC2626)  // error
val DangerRedContainer  = Color(0xFFFFDAD6)  // errorContainer
val OnDangerRed         = Color(0xFFFFFFFF)  // onError
val OnDangerRedContainer = Color(0xFF410002) // onErrorContainer

// Neutral surfaces — light
val Background          = Color(0xFFF8FAFC)
val OnBackground        = Color(0xFF0F172A)
val Surface             = Color(0xFFFFFFFF)
val SurfaceVariant      = Color(0xFFE2E8F0)
val OnSurface           = Color(0xFF0F172A)
val OnSurfaceVariant    = Color(0xFF475569)
val Outline             = Color(0xFFCBD5E1)
val OutlineVariant      = Color(0xFFE2E8F0)

// ── Dark scheme ────────────────────────────────────────────────────────────────
val CivicNavyDark          = Color(0xFF93C5FD)
val CivicNavyContainerDark = Color(0xFF1E3A5F)
val OnCivicNavyDark        = Color(0xFF002244)
val OnCivicNavyContainerDark = Color(0xFFBFD7FF)

val SignalTealDark          = Color(0xFF5EEAD4)
val SignalTealContainerDark = Color(0xFF005252)
val OnSignalTealDark        = Color(0xFF003737)
val OnSignalTealContainerDark = Color(0xFFB2DFDE)

val AlertAmberDark          = Color(0xFFFBBF24)
val AlertAmberContainerDark = Color(0xFF5C3900)
val OnAlertAmberDark        = Color(0xFF1C1000)
val OnAlertAmberContainerDark = Color(0xFFFFDFA5)

val DangerRedDark          = Color(0xFFFF7070)
val DangerRedContainerDark = Color(0xFF7F0000)

// Neutral surfaces — dark
val BackgroundDark       = Color(0xFF0F172A)
val OnBackgroundDark     = Color(0xFFE2E8F0)
val SurfaceDark          = Color(0xFF1E293B)
val SurfaceVariantDark   = Color(0xFF334155)
val OnSurfaceDark        = Color(0xFFE2E8F0)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark          = Color(0xFF475569)
val OutlineVariantDark   = Color(0xFF334155)

// ── Semantic aliases (used directly across the app) ───────────────────────────
val ColorSuccess    = Color(0xFF16A34A)
val ColorWarning    = Color(0xFFF59E0B)
val ColorDanger     = Color(0xFFDC2626)
val ColorInfo       = Color(0xFF0EA5E9)

// Status badge colors
val StatusSubmitted = Color(0xFF64748B)
val StatusTriaged   = Color(0xFF7C3AED)
val StatusAssigned  = Color(0xFF2563EB)
val StatusInProgress = Color(0xFFF59E0B)
val StatusResolved  = Color(0xFF16A34A)
