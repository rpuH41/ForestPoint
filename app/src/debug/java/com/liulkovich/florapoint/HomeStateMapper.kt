
package com.liulkovich.florapoint

import com.liulkovich.florapoint.domain.toReferenceModel
import com.liulkovich.florapoint.presentation.screens.home.HomeScreenState
import com.liulkovich.florapoint.shared.domain.SharedHomeScreenState

fun HomeScreenState.toSharedHomeScreenState(
    language: String
): SharedHomeScreenState {

    val localizedTipText = tip?.let { currentTip ->
        if (language == "en") {
            currentTip.textEn
                ?.takeIf { it.isNotBlank() }
                ?: currentTip.textRu
        } else {
            currentTip.textRu
        }
    }

    return SharedHomeScreenState(
        species = species.map { reference ->
            reference.toReferenceModel()
        },
        tipText = localizedTipText,
        isLoading = isLoading,
        isTipLoading = isTipLoading,
        selectedSeasonCategory = selectedSeasonCategory
    )
}
