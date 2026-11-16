
package com.liulkovich.florapoint.shared.domain

data class SharedNotificationSpecies(
    val id: Int,
    val displayName: String,
    val imageName: String,
    val startMonth: Int,
    val endMonth: Int
)

data class SharedNotificationSettingsState(
    val notifyStart: Boolean = true,
    val notifyPeak: Boolean = true,
    val notifyEnd: Boolean = true,
    val enabledSpecies: List<SharedNotificationSpecies> = emptyList()
)
