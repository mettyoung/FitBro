package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.cache.CacheSource
import com.mettyoung.fitbro.data.model.BodyMetric
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.ExpenditureEstimate
import com.mettyoung.fitbro.data.model.TrendSummary
import com.mettyoung.fitbro.data.model.WeekToDateBalance
import com.mettyoung.fitbro.data.model.Week
import com.mettyoung.fitbro.data.model.WeekGoals
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.WeekSummary

data class DashboardState(
    val uiState: DashboardUiState,
    val lastSyncTime: Map<CacheSource, Long?>,
    val errorMessage: String?,
    val weekStart: WeekStart,
    val selectedWeek: Week,
    /** Selected week's numbers (cardio minutes filled in by the UI from CardioState). */
    val weekSummary: WeekSummary = WeekSummary.EMPTY,
    /** Previous week, like-for-like with the selected week (FR-7). */
    val previousWeekSummary: WeekSummary = WeekSummary.EMPTY,
    /** Previous week's daily balances (all 7 days), for the day-by-day comparison chart. */
    val previousWeekBalances: List<DailyBalance> = emptyList(),
    val showCarbsFat: Boolean = false,
    /** Average daily macro goals over the week's averaging days; null if unavailable. */
    val weekGoals: WeekGoals? = null,
    /** FR-3: completed, logged days of the selected week. */
    val weekToDate: WeekToDateBalance? = null,
    val resistanceTarget: Int = 3,
    val cardioMinutesTarget: Int = 150,
    val body: BodyTrends = BodyTrends()
)

enum class BodyAccess { OK, NO_PERMISSION, UNAVAILABLE }

/** Dashboard body-composition section (US-006/007). */
data class BodyTrends(
    val access: BodyAccess = BodyAccess.OK,
    val trends: Map<BodyMetric, TrendSummary> = emptyMap(),
    val estimate: ExpenditureEstimate? = null,
    /** Rolling estimate per day of the selected week (null where not computable). */
    val estimateSeries: List<Double?> = emptyList()
)
