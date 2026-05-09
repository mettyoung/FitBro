package com.mettyoung.fitbro.ui.dashboard

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
import com.mettyoung.fitbro.data.food.OpenFoodFactsDataSource
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.settings.MacroGoalsSettings

@Composable
fun DashboardWithTabs(
    stateHolder: DashboardStateHolder,
    foodDiaryStateHolder: FoodDiaryStateHolder,
    openFoodFactsDataSource: OpenFoodFactsDataSource,
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
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Balance") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MiOrange,
                        selectedTextColor = MiOrange,
                        indicatorColor = MiOrange.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Macros") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MiOrange,
                        selectedTextColor = MiOrange,
                        indicatorColor = MiOrange.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MiOrange,
                        selectedTextColor = MiOrange,
                        indicatorColor = MiOrange.copy(alpha = 0.1f)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
            when (selectedTabIndex) {
                0 -> DashboardScreen(stateHolder = stateHolder)
                1 -> MacroDailyCounterDetail(
                    userSettingsDataSource = userSettingsDataSource,
                    foodDiaryStateHolder = foodDiaryStateHolder,
                    openFoodFactsDataSource = openFoodFactsDataSource,
                    onDateSelected = stateHolder::setSelectedDate
                )
                2 -> MacroGoalsSettings(userSettingsDataSource = userSettingsDataSource)
            }
        }
    }
}
