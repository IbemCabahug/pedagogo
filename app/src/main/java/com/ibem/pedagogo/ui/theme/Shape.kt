package com.ibem.pedagogo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Web Desk radius port (style.css :root): 8 / 14 / 20 / 28 (design-research.md section 6).
// Cards sit at large (20dp), bottom sheets & QR dialogs at extraLarge (28dp).
val PedagogoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
