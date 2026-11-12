
package com.liulkovich.florapoint.shared.domain

enum class SharedSettingsLanguage {
    SYSTEM,
    RUSSIAN,
    ENGLISH
}

data class SharedSettingsState(
    val isAuthorized: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val language: SharedSettingsLanguage = SharedSettingsLanguage.SYSTEM,
    val appVersion: String = ""
)
