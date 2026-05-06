package com.mettyoung.fitbro.util

private fun isLeapYear(y: Int) = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)

fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 0
}

// 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun (Zeller's congruence)
fun dayOfWeekMonBased(year: Int, month: Int, day: Int): Int {
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val k = y % 100
    val j = y / 100
    val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j).mod(7)
    return (h + 5) % 7
}

private fun String.toDaysSinceEpoch(): Int {
    val parts = split("-")
    val y = parts[0].toInt(); val m = parts[1].toInt(); val d = parts[2].toInt()
    var days = 0
    for (year in 1970 until y) days += if (isLeapYear(year)) 366 else 365
    for (month in 1 until m) days += daysInMonth(y, month)
    return days + d - 1
}

private fun Int.toDayString(): String {
    var remaining = this
    var year = 1970
    while (true) {
        val diy = if (isLeapYear(year)) 366 else 365
        if (remaining < diy) break
        remaining -= diy; year++
    }
    var month = 1
    while (true) {
        val dim = daysInMonth(year, month)
        if (remaining < dim) break
        remaining -= dim; month++
    }
    val day = remaining + 1
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

fun String.plusDays(n: Int): String = (toDaysSinceEpoch() + n).toDayString()
fun String.minusDays(n: Int): String = plusDays(-n)

fun todayString(): String = (currentEpochMs() / 86_400_000L).toInt().toDayString()

fun String.toYMD(): Triple<Int, Int, Int> {
    val p = split("-")
    return Triple(p[0].toInt(), p[1].toInt(), p[2].toInt())
}

fun String.toDisplayRange(): String {
    val (sy, sm, sd) = toYMD()
    val end = plusDays(6)
    val (ey, em, ed) = end.toYMD()
    val startStr = "${MONTH_ABBR[sm]} $sd"
    return if (sm == em && sy == ey) "$startStr – $ed" else "$startStr – ${MONTH_ABBR[em]} $ed"
}

val MONTH_ABBR = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
