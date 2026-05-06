package com.mettyoung.fitbro.ui.dashboard

internal fun buildWarnings(
    healthIntakeFailed: Boolean,
    healthMetabolismFailed: Boolean,
    healthFailed: Boolean
): List<String> = buildList {
    if (healthIntakeFailed) add("Health intake data unavailable")
    if (healthMetabolismFailed) add("BMR data unavailable")
    if (healthFailed) add("Activity data unavailable — balance excludes NEAT/EAT")
}
