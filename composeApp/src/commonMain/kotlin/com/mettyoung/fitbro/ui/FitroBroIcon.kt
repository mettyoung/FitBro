package com.mettyoung.fitbro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FitroBroIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "FitroBro",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Head
        path(
            fill = SolidColor(Color(0xFFFF6600)),
            fillAlpha = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            curveTo(7f, 8.76f, 9.24f, 11f, 12f, 11f)
            curveTo(14.76f, 11f, 17f, 8.76f, 17f, 6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            close()
        }
        // Body + arms (buff silhouette)
        path(
            fill = SolidColor(Color(0xFFFF6600)),
            fillAlpha = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            // Neck left
            moveTo(10f, 11f)
            lineTo(10f, 12.5f)
            // Left shoulder curve (deltoid)
            quadTo(6f, 13f, 4f, 15.5f)
            // Left arm (bicep bulge out then forearm)
            quadTo(1.5f, 18f, 2f, 21f)
            // Left arm base
            lineTo(2f, 23f)
            lineTo(7f, 23f)
            // Left torso
            lineTo(8f, 24f)
            // Bottom
            lineTo(16f, 24f)
            // Right torso
            lineTo(17f, 23f)
            // Right arm base
            lineTo(22f, 23f)
            lineTo(22f, 21f)
            // Right arm (bicep)
            quadTo(22.5f, 18f, 20f, 15.5f)
            // Right shoulder curve
            quadTo(18f, 13f, 14f, 12.5f)
            // Neck right
            lineTo(14f, 11f)
            close()
        }
    }.build()
}
