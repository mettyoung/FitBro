package com.mettyoung.fitbro.util

actual fun currentEpochMs(): Long = System.currentTimeMillis()
actual fun localDateString(): String = java.time.LocalDate.now().toString()
actual fun localHourOfDay(): Int = java.time.LocalTime.now().hour

actual fun formatLocalTime(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault())
        .toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
