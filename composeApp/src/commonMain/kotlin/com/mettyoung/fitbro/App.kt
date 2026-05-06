package com.mettyoung.fitbro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mettyoung.fitbro.data.cache.createCacheDataSource
import com.mettyoung.fitbro.data.health.createHealthDataSource
import com.mettyoung.fitbro.data.repository.CalorieMathRepositoryImpl
import com.mettyoung.fitbro.ui.dashboard.DashboardScreen
import com.mettyoung.fitbro.ui.dashboard.DashboardStateHolder
import com.mettyoung.fitbro.ui.dashboard.DateRange
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.todayString

@Composable
fun App() {
    MaterialTheme {
        val healthDataSource = remember { createHealthDataSource() }
        val cacheDataSource = remember { createCacheDataSource() }
        val calorieMathRepository = remember { CalorieMathRepositoryImpl() }
        val scope = rememberCoroutineScope()
        val initialDateRange = remember {
            val today = todayString()
            DateRange(today.minusDays(6), today)
        }
        val stateHolder = remember(healthDataSource, cacheDataSource, calorieMathRepository, scope) {
            DashboardStateHolder(
                healthDataSource = healthDataSource,
                cacheDataSource = cacheDataSource,
                calorieMathRepository = calorieMathRepository,
                scope = scope,
                initialDateRange = initialDateRange
            )
        }

        LaunchedEffect(Unit) {
            stateHolder.refresh()
        }

        DashboardScreen(
            stateHolder = stateHolder,
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
        )
    }
}
