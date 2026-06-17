package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.MacroGoalProfile
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.toYMD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MacroGoalRepositoryImpl(
    private val database: FitBroDatabase,
    private val userSettingsDataSource: UserSettingsDataSource
) : MacroGoalRepository {

    override fun getAllProfiles(): Flow<List<MacroGoalProfile>> =
        database.macroGoalProfileQueries.getAllProfiles()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getAllMappings(): Flow<Map<Int, Long>> =
        database.macroGoalProfileQueries.getAllMappings()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.associate { it.weekday.toInt() to it.profile_id } }

    override suspend fun addProfile(
        name: String,
        protein: Double,
        carbs: Double,
        fat: Double,
        calories: Double
    ): Long = withContext(Dispatchers.Default) {
        database.macroGoalProfileQueries.transactionWithResult {
            database.macroGoalProfileQueries.insertProfile(
                name = name,
                protein_g = protein,
                carbs_g = carbs,
                fat_g = fat,
                calories_kcal = calories
            )
            database.macroGoalProfileQueries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun updateProfile(
        id: Long,
        name: String,
        protein: Double,
        carbs: Double,
        fat: Double,
        calories: Double
    ): Unit = withContext(Dispatchers.Default) {
        database.macroGoalProfileQueries.updateProfile(
            name = name,
            protein_g = protein,
            carbs_g = carbs,
            fat_g = fat,
            calories_kcal = calories,
            id = id
        )
    }

    override suspend fun deleteProfile(id: Long): Unit = withContext(Dispatchers.Default) {
        database.macroGoalProfileQueries.deleteProfile(id)
    }

    override suspend fun getMappingForWeekday(weekday: Int): Long? = withContext(Dispatchers.Default) {
        database.macroGoalProfileQueries.getMappingForWeekday(weekday.toLong())
            .executeAsOneOrNull()
    }

    override suspend fun setMappingForWeekday(weekday: Int, profileId: Long): Unit =
        withContext(Dispatchers.Default) {
            database.macroGoalProfileQueries.setMappingForWeekday(
                weekday = weekday.toLong(),
                profile_id = profileId
            )
        }

    override suspend fun getActiveProfileForDate(date: String): MacroGoalProfile =
        withContext(Dispatchers.Default) {
            val (year, month, day) = date.toYMD()
            val weekday = dayOfWeekMonBased(year, month, day)
            val profileId = database.macroGoalProfileQueries
                .getMappingForWeekday(weekday.toLong())
                .executeAsOneOrNull()
            val row = if (profileId != null) {
                database.macroGoalProfileQueries.getProfileById(profileId).executeAsOneOrNull()
            } else null
            row?.toDomain()
                ?: database.macroGoalProfileQueries.getDefaultProfile().executeAsOneOrNull()?.toDomain()
                ?: database.macroGoalProfileQueries.getFirstProfile().executeAsOneOrNull()?.toDomain()
                ?: MacroGoalProfile(
                    id = 0,
                    name = "Default",
                    proteinG = userSettingsDataSource.getProteinGoalG(),
                    carbsG = userSettingsDataSource.getCarbsGoalG(),
                    fatG = userSettingsDataSource.getFatGoalG(),
                    caloriesKcal = userSettingsDataSource.getCalorieGoalKcal()
                )
        }

    suspend fun seedDefaultIfEmpty(): Unit = withContext(Dispatchers.Default) {
        val count = database.macroGoalProfileQueries.countProfiles().executeAsOne()
        if (count == 0L) {
            database.macroGoalProfileQueries.insertProfile(
                name = "Default",
                protein_g = userSettingsDataSource.getProteinGoalG(),
                carbs_g = userSettingsDataSource.getCarbsGoalG(),
                fat_g = userSettingsDataSource.getFatGoalG(),
                calories_kcal = userSettingsDataSource.getCalorieGoalKcal()
            )
        }
    }
}

private fun com.mettyoung.fitbro.data.db.MacroGoalProfile.toDomain() = MacroGoalProfile(
    id = id,
    name = name,
    proteinG = protein_g,
    carbsG = carbs_g,
    fatG = fat_g,
    caloriesKcal = calories_kcal
)
