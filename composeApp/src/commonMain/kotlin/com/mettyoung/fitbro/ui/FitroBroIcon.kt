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
        path(
            fill = SolidColor(Color(0xFFFF6600)),
            fillAlpha = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            // Flexed arm: Upper arm and Bicep bulge
            moveTo(20.31f, 14.26f)
            curveTo(19.23f, 12.13f, 17.51f, 10.45f, 14.8f, 9.61f)
            curveTo(12.11f, 8.77f, 9.59f, 9.77f, 9.59f, 9.77f)
            curveTo(9.59f, 9.77f, 7.55f, 8.59f, 5.71f, 10.66f)
            curveTo(3.87f, 12.73f, 3.42f, 15.65f, 3.42f, 15.65f)
            lineTo(3.42f, 21.2f)
            lineTo(20.31f, 21.2f)
            curveTo(20.31f, 21.2f, 21.39f, 16.39f, 20.31f, 14.26f)
            close()
            
            // Forearm and Fist
            moveTo(11.1f, 9.6f)
            curveTo(11.1f, 9.6f, 13.08f, 6.55f, 17.04f, 5.48f)
            curveTo(21f, 4.41f, 22.06f, 7.37f, 22.06f, 7.37f)
            lineTo(23.4f, 10.82f)
            curveTo(23.4f, 10.82f, 21.37f, 12.53f, 17.41f, 13.6f)
            curveTo(13.45f, 14.67f, 11.1f, 9.6f, 11.1f, 9.6f)
            close()
        }
    }.build()
}
