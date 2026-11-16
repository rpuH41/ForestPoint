package com.liulkovich.florapoint.shared.domain

fun SharedFloraCategory.localizedName(language: String): String {
    val english = language == "en"

    return when (this) {
        SharedFloraCategory.MUSHROOM ->
            if (english) "Mushrooms" else "Грибы"

        SharedFloraCategory.BERRY ->
            if (english) "Berries" else "Ягоды"

        SharedFloraCategory.PLANT ->
            if (english) "Plants" else "Растения"

        SharedFloraCategory.NUT ->
            if (english) "Nuts" else "Орехи"

        SharedFloraCategory.OTHER ->
            if (english) "Other" else "Другое"
    }
}
