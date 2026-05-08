package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.model.DailyBalance

@Composable
fun DashboardWithTabs(
    stateHolder: DashboardStateHolder,
    balances: List<DailyBalance>,
    userSettingsDataSource: UserSettingsDataSource,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 }
            ) {
                Text("Calories", modifier = Modifier.padding(16.dp))
            }
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 }
            ) {
                Text("Macros", modifier = Modifier.padding(16.dp))
            }
        }

        when (selectedTabIndex) {
            0 -> DashboardScreen(stateHolder = stateHolder)
            1 -> MacroDailyCounterDetail(
                balances = balances,
                userSettingsDataSource = userSettingsDataSource
            )
        }
    }
}
