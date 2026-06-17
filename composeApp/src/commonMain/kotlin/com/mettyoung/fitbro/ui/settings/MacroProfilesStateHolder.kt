package com.mettyoung.fitbro.ui.settings

import com.mettyoung.fitbro.data.model.MacroGoalProfile
import com.mettyoung.fitbro.data.repository.MacroGoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MacroProfilesState(
    val profiles: List<MacroGoalProfile> = emptyList(),
    val weekdayMappings: Map<Int, Long> = emptyMap()
)

class MacroProfilesStateHolder(
    private val repository: MacroGoalRepository,
    private val scope: CoroutineScope
) {
    val state: StateFlow<MacroProfilesState> = combine(
        repository.getAllProfiles(),
        repository.getAllMappings()
    ) { profiles, mappings ->
        MacroProfilesState(profiles = profiles, weekdayMappings = mappings)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MacroProfilesState()
    )

    fun addProfile(name: String, protein: Double, carbs: Double, fat: Double, calories: Double): Job =
        scope.launch { repository.addProfile(name, protein, carbs, fat, calories) }

    fun updateProfile(id: Long, name: String, protein: Double, carbs: Double, fat: Double, calories: Double): Job =
        scope.launch { repository.updateProfile(id, name, protein, carbs, fat, calories) }

    fun deleteProfile(id: Long): Job = scope.launch { repository.deleteProfile(id) }

    fun setMapping(weekday: Int, profileId: Long): Job =
        scope.launch { repository.setMappingForWeekday(weekday, profileId) }

    suspend fun isMappedToAnyWeekday(profileId: Long): Boolean {
        return state.value.weekdayMappings.values.any { it == profileId }
    }
}
