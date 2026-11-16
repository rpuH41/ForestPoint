package com.liulkovich.florapoint.presentation.screens.guide

import com.liulkovich.florapoint.domain.toReferenceModel
import com.liulkovich.florapoint.shared.domain.SharedGuideScreenState
import com.liulkovich.florapoint.shared.domain.SharedGuideCommand

fun GuideScreenState.toSharedGuideState(): SharedGuideScreenState {
    return SharedGuideScreenState(
        query = query,
        species = species.map { reference ->
            reference.toReferenceModel()
        },
        selectedCategories = selectedCategories,
        isLoading = isLoading
    )
}

fun SharedGuideCommand.toAndroidCommand(): GuideCommand =
    when (this) {
        is SharedGuideCommand.InputSearchQuery ->
            GuideCommand.InputSearchQuery(query)

        is SharedGuideCommand.CheckCategory ->
            GuideCommand.CheckCategory(check)

        is SharedGuideCommand.ToggleNotification ->
            GuideCommand.ToggleNotification(id, enabled)
    }