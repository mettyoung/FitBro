package com.mettyoung.fitbro.ui.dashboard

internal fun buildWarnings(
    cronometerIntakeFailed: Boolean,
    cronometerMetabolismFailed: Boolean,
    healthFailed: Boolean
): List<String> = buildList {
    if (cronometerIntakeFailed && cronometerMetabolismFailed) {
        add("Cronometer data unavailable — showing cached data where possible")
    } else {
        if (cronometerIntakeFailed) add("Calorie intake data unavailable")
        if (cronometerMetabolismFailed) add("Metabolic rate data unavailable")
    }
    if (healthFailed) add("Activity data unavailable — balance excludes NEAT/EAT")
}
