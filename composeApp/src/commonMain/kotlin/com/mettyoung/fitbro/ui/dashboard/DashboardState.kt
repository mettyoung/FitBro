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
    /** 28-day window (same as the body trends) for the Expenditure detail chart. */
    val estimateWindowDays: List<String> = emptyList(),
    /** Rolling estimate per window day (null where not computable). */
    val estimateWindowSeries: List<Double?> = emptyList(),
    /** Logged intake per window day (null = not logged). */
    val windowIntake: List<Double?> = emptyList()
)
