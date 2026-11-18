package com.mettyoung.fitbro.util

expect fun currentEpochMs(): Long
expect fun localDateString(): String
expect fun localHourOfDay(): Int
/** Local wall-clock time "HH:mm" for an epoch-millis instant. */
expect fun formatLocalTime(epochMs: Long): String
