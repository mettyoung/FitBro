package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.DailyBalance

/** Lens applied to the dashboard Balance screen. Presentation-only, in-memory. */
enum class DashboardViewMode(val label: String) {
    BALANCE("Balance"),
    INTAKE("Intake"),
    EXPENDITURE("Expenditure")
}

/** Per-day value plotted/aggregated for this lens. */
fun DashboardViewMode.valueOf(balance: DailyBalance): Double = when (this) {
    DashboardViewMode.BALANCE -> balance.balance
    DashboardViewMode.INTAKE -> balance.intake
    DashboardViewMode.EXPENDITURE -> balance.burn
}

/** BALANCE plots diverging bars around zero; the other lenses are upward-only. */
val DashboardViewMode.isDiverging: Boolean
    get() = this == DashboardViewMode.BALANCE
