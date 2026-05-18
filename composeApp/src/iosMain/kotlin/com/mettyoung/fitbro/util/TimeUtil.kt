package com.mettyoung.fitbro.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun localDateString(): String {
    val cal = NSCalendar.currentCalendar
    val comps = cal.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = NSDate()
    )
    val y = comps.year
    val m = comps.month.toString().padStart(2, '0')
    val d = comps.day.toString().padStart(2, '0')
    return "$y-$m-$d"
}
