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
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
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
import com.mettyoung.fitbro.ui.settings.WeekStartSettings
import com.mettyoung.fitbro.ui.settings.WeeklyGoalsSettings
import com.mettyoung.fitbro.ui.BackHandlerEffect
import com.mettyoung.fitbro.ui.settings.WorkoutTypesSettings
import com.mettyoung.fitbro.ui.settings.ResistanceSourcesSettings
import com.mettyoung.fitbro.data.model.partialWeekSpan
import com.mettyoung.fitbro.util.todayString

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
    var selectedTabIndex by rememberSaveable { mutableStateOf(AppTab.DASHBOARD.ordinal) }
    var moreDestination by remember { mutableStateOf<MoreDestination?>(null) }
    BackHandlerEffect(enabled = selectedTabIndex == AppTab.MORE.ordinal && moreDestination != null) { moreDestination = null }
    // Cardio opened from the Dashboard's cardio card gets a back button; via the tab it doesn't.
    var cardioFromDashboard by remember { mutableStateOf(false) }
    BackHandlerEffect(enabled = selectedTabIndex == AppTab.CARDIO.ordinal && cardioFromDashboard) {
        cardioFromDashboard = false
        selectedTabIndex = AppTab.DASHBOARD.ordinal
    }

    val dashboardDate by stateHolder.selectedDate.collectAsState()
    LaunchedEffect(dashboardDate) {
        foodDiaryStateHolder.setDate(dashboardDate)
    }

    val dashboardState by stateHolder.state.collectAsState()
    // US-004: Cardio follows the Trends week.
    LaunchedEffect(dashboardState.selectedWeek) {
        cardioStateHolder.setWeek(dashboardState.selectedWeek)
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
                        onClick = {
                            // Re-tapping More returns to its list.
                            if (tab == AppTab.MORE) moreDestination = null
                            cardioFromDashboard = false
                            selectedTabIndex = index
                        },
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
                    AppTab.DASHBOARD -> DashboardScreen(
                        stateHolder = stateHolder,
                        cardioSessions = cardioState.sessions,
                        resistanceCount = cardioState.resistanceCount,
                        strengthSessions = cardioState.strengthSessions,
                        workoutDetail = cardioState.workoutDetail,
                        onOpenWorkout = cardioStateHolder::openWorkout,
                        onCloseWorkout = cardioStateHolder::closeWorkout,
                        resistanceSources = cardioState.resistanceSources,
                        excludedResistanceSources = cardioState.excludedResistanceSources,
                        onToggleResistanceSource = cardioStateHolder::setResistanceSourceIncluded,
                        onOpenCardio = {
                            cardioFromDashboard = true
                            selectedTabIndex = AppTab.CARDIO.ordinal
                        }
                    )
                    AppTab.FOOD_LOG -> MacroDailyCounterDetail(
                        macroGoalRepository = macroGoalRepository,
                        foodDiaryStateHolder = foodDiaryStateHolder,
                        customMealStateHolder = customMealStateHolder,
                        customFoodStateHolder = customFoodStateHolder,
                        foodDataSource = foodDataSource,
                        customFoodRepository = customFoodRepository,
                        onDateSelected = stateHolder::setSelectedDate,
                        onBalanceRefreshNeeded = stateHolder::refresh
                    )
                    AppTab.CARDIO -> CardioScreen(
                        stateHolder = cardioStateHolder,
                        weekLabel = dashboardState.selectedWeek.label(todayString(), dashboardState.weekStart),
                        partialSpan = partialWeekSpan(dashboardState.selectedWeek, todayString(), dashboardState.weekStart, includeToday = true),
                        onBack = if (cardioFromDashboard) {
                            {
                                cardioFromDashboard = false
                                selectedTabIndex = AppTab.DASHBOARD.ordinal
                            }
                        } else null
                    )
                    AppTab.MORE -> when (moreDestination) {
                        null -> MoreScreen(onOpen = { moreDestination = it })
                        MoreDestination.COMPARE -> WeekComparisonScreen(
                            stateHolder = stateHolder,
                            cardioSessions = cardioState.sessions,
                            previousCardioSessions = cardioState.previousWeekSessions,
                            onBack = { moreDestination = null }
                        )
                        MoreDestination.SETTINGS -> MacroProfilesSettings(
                            stateHolder = macroProfilesStateHolder,
                            onBack = { moreDestination = null },
                            footer = {
                                WeeklyGoalsSettings(
                                    resistanceTarget = dashboardState.resistanceTarget,
                                    cardioMinutesTarget = dashboardState.cardioMinutesTarget,
                                    onResistanceChange = stateHolder::setResistanceTarget,
                                    onCardioChange = stateHolder::setCardioMinutesTarget
                                )
                                WeekStartSettings(
                                    weekStart = dashboardState.weekStart,
                                    onWeekStartChange = stateHolder::setWeekStart
                                )
                                ResistanceSourcesSettings(cardioStateHolder)
                                WorkoutTypesSettings(cardioStateHolder)
                                CloudSyncSettings(syncRepository, syncSettings)
                            }
                        )
                    }
                }
            }
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    FOOD_LOG("Food log", Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    CARDIO("Cardio", Icons.AutoMirrored.Outlined.DirectionsRun, Icons.AutoMirrored.Filled.DirectionsRun),
    MORE("More", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz),
}
