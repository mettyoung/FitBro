package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.repository.CustomFoodRepository
import com.mettyoung.fitbro.data.repository.MacroGoalRepository
import com.mettyoung.fitbro.data.sync.SyncRepository
import com.mettyoung.fitbro.data.sync.SyncSettings
import com.mettyoung.fitbro.ui.settings.CloudSyncSettings
import com.mettyoung.fitbro.ui.settings.MacroProfilesSettings
import com.mettyoung.fitbro.ui.settings.MacroProfilesStateHolder

@Composable
fun DashboardWithTabs(
    stateHolder: DashboardStateHolder,
    foodDiaryStateHolder: FoodDiaryStateHolder,
    customMealStateHolder: CustomMealStateHolder,
    customFoodStateHolder: CustomFoodStateHolder,
    cardioStateHolder: CardioStateHolder,
    macroProfilesStateHolder: MacroProfilesStateHolder,
    macroGoalRepository: MacroGoalRepository,
    syncRepository: SyncRepository,
    syncSettings: SyncSettings,
    foodDataSource: FoodDataSource,
    customFoodRepository: CustomFoodRepository,
    balances: List<DailyBalance>,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(AppTab.TODAY.ordinal) }

    val dashboardDate by stateHolder.selectedDate.collectAsState()
    LaunchedEffect(dashboardDate) {
        foodDiaryStateHolder.setDate(dashboardDate)
    }

    val dashboardState by stateHolder.state.collectAsState()
    LaunchedEffect(dashboardState.selectedDateRange) {
        cardioStateHolder.setDateRange(dashboardState.selectedDateRange)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                AppTab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = null
                            )
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                targetState = AppTab.entries[selectedTabIndex],
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { tab ->
                val cardioState by cardioStateHolder.state.collectAsState()
                when (tab) {
                    AppTab.TODAY -> MacroDailyCounterDetail(
                        macroGoalRepository = macroGoalRepository,
                        foodDiaryStateHolder = foodDiaryStateHolder,
                        customMealStateHolder = customMealStateHolder,
                        customFoodStateHolder = customFoodStateHolder,
                        foodDataSource = foodDataSource,
                        customFoodRepository = customFoodRepository,
                        onDateSelected = stateHolder::setSelectedDate,
                        onBalanceRefreshNeeded = stateHolder::refresh
                    )
                    AppTab.TRENDS -> DashboardScreen(
                        stateHolder = stateHolder,
                        weeklyCardioMinutes = cardioState.totalMinutes
                    )
                    AppTab.CARDIO -> CardioScreen(stateHolder = cardioStateHolder)
                    AppTab.SETTINGS -> MacroProfilesSettings(
                        stateHolder = macroProfilesStateHolder,
                        footer = { CloudSyncSettings(syncRepository, syncSettings) }
                    )
                }
            }
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    TODAY("Today", Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    TRENDS("Trends", Icons.Outlined.Insights, Icons.Filled.Insights),
    CARDIO("Cardio", Icons.AutoMirrored.Outlined.DirectionsRun, Icons.AutoMirrored.Filled.DirectionsRun),
    SETTINGS("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}
