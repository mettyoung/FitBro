package com.mettyoung.fitbro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mettyoung.fitbro.data.cache.createCacheDataSource
import com.mettyoung.fitbro.data.cache.createUserSettingsDataSource
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.db.createSqlDriver
import com.mettyoung.fitbro.data.food.FoodDatabase
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.food.FatSecretFoodDataSource
import com.mettyoung.fitbro.data.food.OpenFoodFactsFoodDataSource
import com.mettyoung.fitbro.data.food.UsdaFoodDataSource
import com.mettyoung.fitbro.data.health.createHealthDataSource
import com.mettyoung.fitbro.data.repository.CalorieMathRepositoryImpl
import com.mettyoung.fitbro.data.repository.FoodDiaryRepositoryImpl
import com.mettyoung.fitbro.ui.FitBroTheme
import com.mettyoung.fitbro.ui.dashboard.DashboardStateHolder
import com.mettyoung.fitbro.ui.dashboard.DashboardUiState
import com.mettyoung.fitbro.ui.dashboard.DashboardWithTabs
import com.mettyoung.fitbro.ui.dashboard.DateRange
import com.mettyoung.fitbro.ui.dashboard.FoodDiaryStateHolder
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.todayString

@Composable
fun App() {
    FitBroTheme {
        val scope = rememberCoroutineScope()
        val healthDataSource = remember { createHealthDataSource() }
        val cacheDataSource = remember { createCacheDataSource() }
        val userSettingsDataSource = remember { createUserSettingsDataSource() }
        val calorieMathRepository = remember { CalorieMathRepositoryImpl() }

        val database = remember {
            FitBroDatabase(createSqlDriver())
        }
        val foodDiaryRepository = remember(database) { FoodDiaryRepositoryImpl(database) }

        var activeFoodDatabase by remember { mutableStateOf(userSettingsDataSource.getFoodDatabase()) }
        val foodDataSource: FoodDataSource = remember(activeFoodDatabase) {
            when (activeFoodDatabase) {
                FoodDatabase.OPEN_FOOD_FACTS -> OpenFoodFactsFoodDataSource()
                FoodDatabase.USDA -> UsdaFoodDataSource()
                FoodDatabase.FATSECRET -> FatSecretFoodDataSource()
            }
        }

        val foodDiaryStateHolder = remember(foodDiaryRepository, healthDataSource, scope) {
            FoodDiaryStateHolder(
                repository = foodDiaryRepository,
                healthDataSource = healthDataSource,
                scope = scope
            )
        }

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

        val state by stateHolder.state.collectAsState()
        val balances = (state.uiState as? DashboardUiState.Success)?.balances ?: emptyList()

        DashboardWithTabs(
            stateHolder = stateHolder,
            foodDiaryStateHolder = foodDiaryStateHolder,
            foodDataSource = foodDataSource,
            balances = balances,
            userSettingsDataSource = userSettingsDataSource,
            selectedFoodDatabase = activeFoodDatabase,
            onFoodDatabaseChanged = { db ->
                userSettingsDataSource.setFoodDatabase(db)
                activeFoodDatabase = db
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
