package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MacroDataSource
import com.mettyoung.fitbro.util.todayString

data class FoodDiaryState(
    val entriesByMeal: Map<String, List<FoodDiaryEntry>>,
    val dailyTotals: DailyMacroTotals,
    val macroDataSource: MacroDataSource,
    val isLoading: Boolean,
    val error: String?
) {
    companion object {
        fun initial(date: String = todayString()) = FoodDiaryState(
            entriesByMeal = emptyMap(),
            dailyTotals = DailyMacroTotals.empty(date),
            macroDataSource = MacroDataSource.FOOD_DIARY,
            isLoading = false,
            error = null
        )
    }
}
