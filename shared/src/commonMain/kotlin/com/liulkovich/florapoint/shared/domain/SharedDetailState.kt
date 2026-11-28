package com.liulkovich.florapoint.shared.domain

data class SharedDetailScreenState(
    val species: ReferenceModel? = null,
    val isNotificationEnabled: Boolean = false,
    val resolvedLookAlikes: Map<String, Int?> = emptyMap()
)
