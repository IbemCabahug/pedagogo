package com.ibem.pedagogo.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec

// Web Desk motion port (style.css :root --transition):
// 0.22s cubic-bezier(0.16, 1, 0.3, 1) - a calm decelerate curve.
// One curve, reused everywhere: calm by consistency (design-research.md section 6).
val PedagogoEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

fun <T> pedagogoTransition(): TweenSpec<T> =
    TweenSpec(durationMillis = 220, easing = PedagogoEasing)
