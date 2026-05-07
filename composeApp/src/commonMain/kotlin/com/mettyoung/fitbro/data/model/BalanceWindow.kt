package com.mettyoung.fitbro.data.model

enum class TrendDirection {
    IMPROVING, STABLE, DECLINING
}

data class BalanceWindow(
    val balances: List<DailyBalance>,
    val avgDailyBalance: Double,
    val totalBalance: Double,
    val trend: TrendDirection
)

fun List<DailyBalance>.groupByWeeks(): List<BalanceWindow> {
    if (isEmpty()) return emptyList()

    val windows = mutableListOf<BalanceWindow>()

    for (i in indices step 7) {
        val endIdx = minOf(i + 7, size)
        val weekBalances = subList(i, endIdx)

        val totalBalance = weekBalances.sumOf { it.balance }
        val avgDailyBalance = if (weekBalances.isNotEmpty()) totalBalance / weekBalances.size else 0.0

        val trend = calculateTrend(weekBalances)

        windows.add(
            BalanceWindow(
                balances = weekBalances,
                avgDailyBalance = avgDailyBalance,
                totalBalance = totalBalance,
                trend = trend
            )
        )
    }

    return windows
}

private fun calculateTrend(balances: List<DailyBalance>): TrendDirection {
    if (balances.size < 6) return TrendDirection.STABLE

    val firstThree = balances.take(3).map { it.balance }.average()
    val lastThree = balances.takeLast(3).map { it.balance }.average()
    val diff = lastThree - firstThree

    return when {
        diff > 50 -> TrendDirection.IMPROVING
        diff < -50 -> TrendDirection.DECLINING
        else -> TrendDirection.STABLE
    }
}
