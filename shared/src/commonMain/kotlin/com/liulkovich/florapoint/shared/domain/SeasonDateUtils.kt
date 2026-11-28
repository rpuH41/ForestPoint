
package com.liulkovich.florapoint.shared.domain

fun countSeasonDaysLeft(
    endMonth: Int,
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int
): Long {
    if (endMonth !in 1..12 || currentMonth !in 1..12) {
        return 0L
    }

    val currentMonthDays = daysInMonth(
        currentYear,
        currentMonth
    )

    if (currentDay !in 1..currentMonthDays) {
        return 0L
    }

    // Сохраняем пока поведение исходной Android-функции.
    if (endMonth < currentMonth) {
        return 0L
    }

    var daysLeft = (currentMonthDays - currentDay).toLong()

    for (month in (currentMonth + 1)..endMonth) {
        daysLeft += daysInMonth(currentYear, month)
    }

    return daysLeft
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        2 -> {
            val leapYear =
                year % 4 == 0 &&
                        (year % 100 != 0 || year % 400 == 0)

            if (leapYear) 29 else 28
        }

        4, 6, 9, 11 -> 30

        else -> 31
    }
}
