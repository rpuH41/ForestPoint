
package com.liulkovich.florapoint.shared.domain

private val dangerousMushroomIds = setOf(
    26, 27, 75, 74, 73, 79, 72
)

fun ReferenceModel.isDangerousMushroom(): Boolean =
    id in dangerousMushroomIds

private val russianMonths = listOf(
    "января",
    "февраля",
    "марта",
    "апреля",
    "мая",
    "июня",
    "июля",
    "августа",
    "сентября",
    "октября",
    "ноября",
    "декабря"
)

fun ReferenceModel.seasonText(
    language: String = "ru"
): String {
    val startIndex = startMonth - 1
    val endIndex = endMonth - 1

    if (startIndex !in russianMonths.indices ||
        endIndex !in russianMonths.indices
    ) {
        return if (language == "en") {
            "Season unknown"
        } else {
            "Сезон неизвестен"
        }
    }

    if (language == "en") {
        val englishMonths = listOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )

        return "Season: ${englishMonths[startIndex]} – ${englishMonths[endIndex]}"
    }

    return "Сезон: ${russianMonths[startIndex]} — ${russianMonths[endIndex]}"
}
