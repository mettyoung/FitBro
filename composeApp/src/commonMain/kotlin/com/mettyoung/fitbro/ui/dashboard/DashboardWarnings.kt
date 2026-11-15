package com.mettyoung.fitbro.ui.dashboard

internal fun buildWarnings(
    healthMetabolismFailed: Boolean,
    healthFailed: Boolean
): List<String> = buildList {
    if (healthMetabolismFailed) add("BMR data unavailable")
    if (healthFailed) add("Activity data unavailable — balance excludes NEAT/EAT")
}
