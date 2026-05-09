package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.util.todayString

data class FoodDiaryState(
    val entriesByMeal: Map<String, List<FoodDiaryEntry>>,
    val dailyTotals: DailyMacroTotals,
    val isLoading: Boolean,
    val error: String?
) {
    companion object {
        fun initial(date: String = todayString()) = FoodDiaryState(
            entriesByMeal = emptyMap(),
            dailyTotals = DailyMacroTotals.empty(date),
            isLoading = false,
            error = null
        )
    }
}
