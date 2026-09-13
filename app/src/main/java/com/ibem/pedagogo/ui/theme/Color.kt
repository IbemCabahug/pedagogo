package com.ibem.pedagogo.ui.theme

import androidx.compose.ui.graphics.Color

// Primary: Calming Sage & Herbal Green (Symbolizing growth, patience, chalkboard nature)
val SagePrimary = Color(0xFF3B6347)
val SageOnPrimary = Color(0xFFFFFFFF)
val SageContainer = Color(0xFFD6E9D8)
val SageOnContainer = Color(0xFF132A1B)

val SagePrimaryDark = Color(0xFFA2CFA9)
val SageOnPrimaryDark = Color(0xFF0F381D)
val SageContainerDark = Color(0xFF244D31)
val SageOnContainerDark = Color(0xFFD6E9D8)

// Secondary: Warm Terracotta & Clay (Human warmth, noble vocation, passion)
val TerracottaSecondary = Color(0xFFBF5F3E)
val TerracottaOnSecondary = Color(0xFFFFFFFF)
val TerracottaContainer = Color(0xFFFDE8E1)
val TerracottaOnContainer = Color(0xFF3E1B0F)

val TerracottaDark = Color(0xFFEE977B)
val TerracottaOnDark = Color(0xFF4E2114)
val TerracottaContainerDark = Color(0xFF672F20)
val TerracottaOnContainerDark = Color(0xFFFDE8E1)

// Tertiary: Morning Honey / Warm Amber (Inspiration, spark of curiosity)
val HoneyTertiary = Color(0xFFD98326)
val HoneyOnTertiary = Color(0xFFFFFFFF)
val HoneyContainer = Color(0xFFFFF0D6)
val HoneyOnContainer = Color(0xFF381D00)

val HoneyDark = Color(0xFFFBB86A)
val HoneyOnDark = Color(0xFF452400)
val HoneyContainerDark = Color(0xFF633800)
val HoneyOnContainerDark = Color(0xFFFFF0D6)

// Background & Surface: Soft Parchment (Light) & Chalkboard Slate (Dark) - hexes aligned to the web Desk root tokens (design-research.md section 3, 2026-09-13)
val ParchmentBackground = Color(0xFFFAF8F3)
val ParchmentSurface = Color(0xFFFFFFFF)
val ParchmentCard = Color(0xFFF4EFE6)
val OnParchmentText = Color(0xFF242B25)
val OnParchmentSubtle = Color(0xFF5B665E)
val ParchmentOutline = Color(0xFFDDD7CB)

val ChalkboardDarkBg = Color(0xFF131714)
val ChalkboardDarkSurface = Color(0xFF1B221C)
val ChalkboardDarkCard = Color(0xFF20261F)
val OnChalkboardText = Color(0xFFE4E7E2)
val OnChalkboardSubtle = Color(0xFFA8B3A9)
val ChalkboardDarkOutline = Color(0xFF3D4A3F)

// Education Category Tag Colors (Harmonious, gentle, non-stressful)
val CatLecture = Color(0xFF3B6347)          // Sage Green
val CatFieldStudy = Color(0xFF2F6F80)        // Deep River Teal
val CatDemoTeaching = Color(0xFFD4683B)      // Warm Coral Amber (empowering, not alarming)
val CatPrepTime = Color(0xFF755B8C)          // Dusty Lavender


// M3 surface-container ladder (light) — parchment undertone, never pure gray
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F4ED)
val SurfaceContainerLight = Color(0xFFF1EDE2)
val SurfaceContainerHighLight = Color(0xFFEBE5D9)
val SurfaceContainerHighestLight = Color(0xFFE5DFD2)
val SurfaceBrightLight = Color(0xFFFCFAF6)
val SurfaceDimLight = Color(0xFFE9E4D9)

// M3 surface-container ladder (dark) — chalkboard with a faint green undertone
val SurfaceContainerLowestDark = Color(0xFF0E120F)
val SurfaceContainerLowDark = Color(0xFF171C18)
val SurfaceContainerDark = Color(0xFF1F2721)
val SurfaceContainerHighDark = Color(0xFF2A332C)
val SurfaceContainerHighestDark = Color(0xFF353F36)
val SurfaceBrightDark = Color(0xFF262F27)
val SurfaceDimDark = Color(0xFF10140F)

// Error: Calm Clay — house rule "no red on people" (design-research.md §3).
// Reserved for system/destructive paths ONLY; learner performance states use
// sage (good) / honey (attention), never this family.
val CalmClayError = Color(0xFF9C4A2F)         // ≈4.5:1 on parchment
val CalmClayOnError = Color(0xFFFFFFFF)
val CalmClayErrorContainer = Color(0xFFFAE4DC)
val CalmClayOnErrorContainer = Color(0xFF421608)

val CalmClayErrorDark = Color(0xFFE8A183)
val CalmClayOnErrorDark = Color(0xFF441507)
val CalmClayErrorContainerDark = Color(0xFF6A3220)
val CalmClayOnErrorContainerDark = Color(0xFFFBE3D9)

// Inverse + helpers (snackbar toasts, scrims, ripple tint)
val InversePrimaryLight = SagePrimaryDark
val InverseSurfaceLight = Color(0xFF2C322D)
val InverseOnSurfaceLight = Color(0xFFEFF1EA)
val InversePrimaryDark = SagePrimary
val InverseSurfaceDark = Color(0xFFE4E7E2)
val InverseOnSurfaceDark = Color(0xFF2C322D)

val OutlineVariantLight = Color(0xFFE6E1D6)   // softer than outline — dividers
val OutlineVariantDark = Color(0xFF2A332C)    // matches the web's dark border-soft
val SurfaceTintLight = SagePrimary
val SurfaceTintDark = SagePrimaryDark
val ScrimColor = Color(0xFF000000)

