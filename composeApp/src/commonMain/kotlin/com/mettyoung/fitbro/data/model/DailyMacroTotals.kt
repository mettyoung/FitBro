package com.mettyoung.fitbro.data.model

data class DailyMacroTotals(
    val date: String,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double
) {
    companion object {
        fun empty(date: String) = DailyMacroTotals(date, 0.0, 0.0, 0.0, 0.0)
    }
}
