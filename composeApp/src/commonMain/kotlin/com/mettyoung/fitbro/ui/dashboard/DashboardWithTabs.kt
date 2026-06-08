package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.settings.MacroGoalsSettings

@Composable
fun DashboardWithTabs(
    stateHolder: DashboardStateHolder,
    foodDiaryStateHolder: FoodDiaryStateHolder,
    customMealStateHolder: CustomMealStateHolder,
    foodDataSource: FoodDataSource,
    balances: List<DailyBalance>,
    userSettingsDataSource: UserSettingsDataSource,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val dashboardDate by stateHolder.selectedDate.collectAsState()
    LaunchedEffect(dashboardDate) {
        foodDiaryStateHolder.setDate(dashboardDate)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    Triple(0, Icons.Default.Info, "Balance"),
                    Triple(1, Icons.AutoMirrored.Filled.List, "Macros"),
                    Triple(2, Icons.Default.Settings, "Settings")
                )

                items.forEach { (index, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MiOrange,
                            selectedTextColor = MiOrange,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> DashboardScreen(stateHolder = stateHolder)
                    1 -> MacroDailyCounterDetail(
                        userSettingsDataSource = userSettingsDataSource,
                        foodDiaryStateHolder = foodDiaryStateHolder,
                        customMealStateHolder = customMealStateHolder,
                        foodDataSource = foodDataSource,
                        onDateSelected = stateHolder::setSelectedDate,
                        onBalanceRefreshNeeded = stateHolder::refresh
                    )
                    2 -> MacroGoalsSettings(
                        userSettingsDataSource = userSettingsDataSource
                    )
                }
            }
        }
    }
}
