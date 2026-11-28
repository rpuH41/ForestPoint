package com.liulkovich.florapoint.presentation.screens.detail

import com.liulkovich.florapoint.domain.toReferenceModel
import com.liulkovich.florapoint.shared.domain.SharedDetailScreenState

fun DetailScreenState.toSharedDetailState(): SharedDetailScreenState {
    return SharedDetailScreenState(
        species = species?.toReferenceModel(),
        isNotificationEnabled = isNotificationEnabled,
        resolvedLookAlikes = resolvedLookAlikes
    )
}
