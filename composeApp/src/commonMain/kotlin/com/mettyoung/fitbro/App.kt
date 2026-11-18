package com.mettyoung.fitbro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.mettyoung.fitbro.data.repository.CustomFoodRepositoryImpl
import com.mettyoung.fitbro.data.repository.CustomMealRepositoryImpl
import com.mettyoung.fitbro.data.repository.FoodDiaryRepositoryImpl
import com.mettyoung.fitbro.data.repository.MacroGoalRepository
import com.mettyoung.fitbro.data.repository.MacroGoalRepositoryImpl
import com.mettyoung.fitbro.data.sync.SyncApi
import com.mettyoung.fitbro.data.sync.SyncRepository
import com.mettyoung.fitbro.data.sync.SyncSettings
import com.mettyoung.fitbro.ui.FitBroTheme
import com.mettyoung.fitbro.ui.settings.MacroProfilesStateHolder
import com.mettyoung.fitbro.ui.dashboard.CardioStateHolder
import com.mettyoung.fitbro.ui.dashboard.CustomFoodStateHolder
import com.mettyoung.fitbro.ui.dashboard.CustomMealStateHolder
import com.mettyoung.fitbro.ui.dashboard.DashboardStateHolder
import com.mettyoung.fitbro.ui.dashboard.DashboardUiState
import com.mettyoung.fitbro.ui.dashboard.DashboardWithTabs
import com.mettyoung.fitbro.ui.dashboard.FoodDiaryStateHolder

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

        val syncSettings = userSettingsDataSource as SyncSettings
        val syncRepository = remember(foodDiaryRepository, healthDataSource, macroGoalRepository, scope) {
            SyncRepository(
                api = SyncApi(syncSettings),
                store = foodDiaryRepository,
                settings = syncSettings,
                healthDataSource = healthDataSource,
                macroGoalRepository = macroGoalRepository,
                scope = scope
            )
        }
        // FR-15: covers app start and every return to the foreground.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { syncRepository.requestSync() }

        val foodDiaryStateHolder = remember(foodDiaryRepository, healthDataSource, scope) {
            FoodDiaryStateHolder(
                repository = foodDiaryRepository,
                healthDataSource = healthDataSource,
                scope = scope,
                onDiaryChanged = syncRepository::requestSync
            )
        }

        val customMealStateHolder = remember(customMealRepository, scope) {
            CustomMealStateHolder(repository = customMealRepository, scope = scope)
        }

        val customFoodStateHolder = remember(customFoodRepository, scope) {
            CustomFoodStateHolder(repository = customFoodRepository, scope = scope)
        }

        val cardioStateHolder = remember(healthDataSource, userSettingsDataSource, scope) {
            CardioStateHolder(
                healthDataSource = healthDataSource,
                userSettingsDataSource = userSettingsDataSource,
                scope = scope
            )
        }
        // Pick up sessions Mi Fitness (etc.) synced to Health Connect while we were in the background.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { cardioStateHolder.refresh() }

        val stateHolder = remember(
            healthDataSource,
            cacheDataSource,
            foodDiaryRepository,
            userSettingsDataSource,
            calorieMathRepository,
            syncRepository,
            scope
        ) {
            DashboardStateHolder(
                healthDataSource = healthDataSource,
                cacheDataSource = cacheDataSource,
                foodDiaryRepository = foodDiaryRepository,
                userSettingsDataSource = userSettingsDataSource,
                calorieMathRepository = calorieMathRepository,
                macroGoalRepository = macroGoalRepository,
                scope = scope,
                onBalancesComputed = syncRepository::publishExpenditure
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
            syncRepository = syncRepository,
            syncSettings = syncSettings,
            foodDataSource = foodDataSource,
            customFoodRepository = customFoodRepository,
            balances = balances,
            modifier = Modifier.fillMaxSize()
        )
    }
}
