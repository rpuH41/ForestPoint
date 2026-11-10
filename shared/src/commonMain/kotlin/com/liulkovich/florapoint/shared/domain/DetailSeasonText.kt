package com.liulkovich.florapoint.shared.domain

fun ReferenceModel.detailSeasonText(language: String = "ru"): String {
    val monthsRu = listOf(
        "января", "февраля", "марта", "апреля",
        "мая", "июня", "июля", "августа",
        "сентября", "октября", "ноября", "декабря"
    )

    val monthsEn = listOf(
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    )

    val start = startMonth
    val end = endMonth

    if (start !in 1..12 || end !in 1..12) {
        return if (language == "en") {
            "Season not specified"
        } else {
            "Период не указан"
        }
    }

    return if (language == "en") {
        "From ${monthsEn[start - 1]} to ${monthsEn[end - 1]}"
    } else {
        "С ${monthsRu[start - 1]} по ${monthsRu[end - 1]}"
    }
}
