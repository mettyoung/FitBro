package com.mettyoung.fitbro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mettyoung.fitbro.data.cache.createCacheDataSource
import com.mettyoung.fitbro.data.cache.createUserSettingsDataSource
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.db.createSqlDriver
import com.mettyoung.fitbro.data.food.CompositeFoodDataSource
import com.mettyoung.fitbro.data.food.CustomFoodDataSource
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.food.FatSecretFoodDataSource
import com.mettyoung.fitbro.data.health.createHealthDataSource
import com.mettyoung.fitbro.data.repository.CalorieMathRepositoryImpl
import com.mettyoung.fitbro.data.repository.CardioRepositoryImpl
import com.mettyoung.fitbro.data.repository.CustomFoodRepositoryImpl
import com.mettyoung.fitbro.data.repository.CustomMealRepositoryImpl
import com.mettyoung.fitbro.data.repository.FoodDiaryRepositoryImpl
import com.mettyoung.fitbro.data.repository.MacroGoalRepository
import com.mettyoung.fitbro.data.repository.MacroGoalRepositoryImpl
import com.mettyoung.fitbro.ui.FitBroTheme
import com.mettyoung.fitbro.ui.settings.MacroProfilesStateHolder
import com.mettyoung.fitbro.ui.dashboard.CardioStateHolder
import com.mettyoung.fitbro.ui.dashboard.CustomFoodStateHolder
import com.mettyoung.fitbro.ui.dashboard.CustomMealStateHolder
import com.mettyoung.fitbro.ui.dashboard.DashboardStateHolder
import com.mettyoung.fitbro.ui.dashboard.DashboardUiState
import com.mettyoung.fitbro.ui.dashboard.DashboardWithTabs
import com.mettyoung.fitbro.ui.dashboard.DateRange
import com.mettyoung.fitbro.ui.dashboard.FoodDiaryStateHolder
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
        val customMealRepository = remember(database) { CustomMealRepositoryImpl(database) }
        val customFoodRepository = remember(database) { CustomFoodRepositoryImpl(database) }
        val cardioRepository = remember(database) { CardioRepositoryImpl(database) }
        val macroGoalRepository: MacroGoalRepository = remember(database, userSettingsDataSource) {
            MacroGoalRepositoryImpl(database, userSettingsDataSource)
        }
        val macroGoalRepositoryImpl = macroGoalRepository as MacroGoalRepositoryImpl
        val macroProfilesStateHolder = remember(macroGoalRepository, scope) {
            MacroProfilesStateHolder(macroGoalRepository, scope)
        }

        val foodDataSource: FoodDataSource = remember(customFoodRepository) {
            CompositeFoodDataSource(
                local = CustomFoodDataSource(customFoodRepository),
                remote = FatSecretFoodDataSource()
            )
        }

        val foodDiaryStateHolder = remember(foodDiaryRepository, healthDataSource, userSettingsDataSource, scope) {
            FoodDiaryStateHolder(
                repository = foodDiaryRepository,
                healthDataSource = healthDataSource,
                userSettingsDataSource = userSettingsDataSource,
                scope = scope
            )
        }

        val customMealStateHolder = remember(customMealRepository, scope) {
            CustomMealStateHolder(repository = customMealRepository, scope = scope)
        }

        val customFoodStateHolder = remember(customFoodRepository, scope) {
            CustomFoodStateHolder(repository = customFoodRepository, scope = scope)
        }

        val cardioStateHolder = remember(cardioRepository, scope) {
            CardioStateHolder(repository = cardioRepository, scope = scope)
        }

        val initialDateRange = remember {
            DateRange(userSettingsDataSource.getDashboardStartDate(), todayString())
        }
        val stateHolder = remember(
            healthDataSource,
            cacheDataSource,
            foodDiaryRepository,
            userSettingsDataSource,
            calorieMathRepository,
            scope
        ) {
            DashboardStateHolder(
                healthDataSource = healthDataSource,
                cacheDataSource = cacheDataSource,
                foodDiaryRepository = foodDiaryRepository,
                userSettingsDataSource = userSettingsDataSource,
                calorieMathRepository = calorieMathRepository,
                scope = scope,
                initialDateRange = initialDateRange
            )
        }

        LaunchedEffect(Unit) {
            macroGoalRepositoryImpl.seedDefaultIfEmpty()
            stateHolder.refresh()
        }

        val state by stateHolder.state.collectAsState()
        val balances = (state.uiState as? DashboardUiState.Success)?.balances ?: emptyList()

        DashboardWithTabs(
            stateHolder = stateHolder,
            foodDiaryStateHolder = foodDiaryStateHolder,
            customMealStateHolder = customMealStateHolder,
            customFoodStateHolder = customFoodStateHolder,
            cardioStateHolder = cardioStateHolder,
            macroProfilesStateHolder = macroProfilesStateHolder,
            macroGoalRepository = macroGoalRepository,
            foodDataSource = foodDataSource,
            customFoodRepository = customFoodRepository,
            balances = balances,
            modifier = Modifier.fillMaxSize()
        )
    }
}
