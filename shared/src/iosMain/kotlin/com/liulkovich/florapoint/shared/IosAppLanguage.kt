
package com.liulkovich.florapoint.shared

import platform.Foundation.NSBundle

fun currentIosLanguage(): String {

    val languageTag = NSBundle.mainBundle
        .preferredLocalizations
        .firstOrNull()
        ?.toString()
        ?.substringBefore("-")
        ?.substringBefore("_")
        ?.lowercase()

    return if (languageTag == "ru") {
        "ru"
    } else {
        "en"
    }
}
