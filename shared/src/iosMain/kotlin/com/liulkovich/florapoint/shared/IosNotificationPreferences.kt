package com.liulkovich.florapoint.shared

import platform.Foundation.NSUserDefaults

/** Persist seasonal notification options locally. Delivery is added separately. */
data class IosNotificationPreferences(
    val notifyStart: Boolean = true,
    val notifyPeak: Boolean = true,
    val notifyEnd: Boolean = true
)

private const val NOTIFY_START_KEY = "ForestPointNotifySeasonStart"
private const val NOTIFY_PEAK_KEY = "ForestPointNotifySeasonPeak"
private const val NOTIFY_END_KEY = "ForestPointNotifySeasonEnd"

fun loadIosNotificationPreferences(): IosNotificationPreferences {
    val defaults = NSUserDefaults.standardUserDefaults

    fun loadFlag(key: String): Boolean =
        if (defaults.objectForKey(key) == null) true else defaults.boolForKey(key)

    return IosNotificationPreferences(
        notifyStart = loadFlag(NOTIFY_START_KEY),
        notifyPeak = loadFlag(NOTIFY_PEAK_KEY),
        notifyEnd = loadFlag(NOTIFY_END_KEY)
    )
}

fun saveIosNotificationPreferences(value: IosNotificationPreferences) {
    val defaults = NSUserDefaults.standardUserDefaults
    defaults.setBool(value.notifyStart, forKey = NOTIFY_START_KEY)
    defaults.setBool(value.notifyPeak, forKey = NOTIFY_PEAK_KEY)
    defaults.setBool(value.notifyEnd, forKey = NOTIFY_END_KEY)
}
