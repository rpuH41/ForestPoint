
package com.liulkovich.florapoint.shared

import com.liulkovich.florapoint.shared.domain.SharedSettingsLanguage
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val FORESTPOINT_LANGUAGE_KEY = "ForestPointSelectedLanguage"
private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

fun selectedIosSettingsLanguage(): SharedSettingsLanguage {
    val saved = NSUserDefaults.standardUserDefaults
        .stringForKey(FORESTPOINT_LANGUAGE_KEY)

    return when (saved) {
        "ru" -> SharedSettingsLanguage.RUSSIAN
        "en" -> SharedSettingsLanguage.ENGLISH
        else -> SharedSettingsLanguage.SYSTEM
    }
}

fun setIosSettingsLanguage(selected: SharedSettingsLanguage) {
    val defaults = NSUserDefaults.standardUserDefaults

    when (selected) {
        SharedSettingsLanguage.RUSSIAN -> {
            defaults.setObject("ru", forKey = FORESTPOINT_LANGUAGE_KEY)
            defaults.setObject(arrayListOf("ru"), forKey = APPLE_LANGUAGES_KEY)
        }
        SharedSettingsLanguage.ENGLISH -> {
            defaults.setObject("en", forKey = FORESTPOINT_LANGUAGE_KEY)
            defaults.setObject(arrayListOf("en"), forKey = APPLE_LANGUAGES_KEY)
        }
        SharedSettingsLanguage.SYSTEM -> {
            defaults.removeObjectForKey(FORESTPOINT_LANGUAGE_KEY)
            defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
        }
    }
}

fun currentIosLanguage(): String {
    when (selectedIosSettingsLanguage()) {
        SharedSettingsLanguage.RUSSIAN -> return "ru"
        SharedSettingsLanguage.ENGLISH -> return "en"
        SharedSettingsLanguage.SYSTEM -> Unit
    }

    val languageTag = NSBundle.mainBundle
        .preferredLocalizations
        .firstOrNull()
        ?.toString()
        ?.substringBefore("-")
        ?.substringBefore("_")
        ?.lowercase()

    return if (languageTag == "ru") "ru" else "en"
}
