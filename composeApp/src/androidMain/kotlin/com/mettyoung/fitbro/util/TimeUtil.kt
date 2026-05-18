package com.mettyoung.fitbro.util

actual fun currentEpochMs(): Long = System.currentTimeMillis()
actual fun localDateString(): String = java.time.LocalDate.now().toString()
