package com.mettyoung.fitbro

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mettyoung.fitbro.ui.LocalIsDarkTheme

/**
 * Keeps status/navigation bar icons readable when the in-app theme differs from the system's
 * (enableEdgeToEdge only follows the system setting).
 */
@Composable
fun SyncSystemBarIcons() {
    val dark = LocalIsDarkTheme.current
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
