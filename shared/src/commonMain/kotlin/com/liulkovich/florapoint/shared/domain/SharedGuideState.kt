package com.liulkovich.florapoint.shared.domain

data class SharedGuideScreenState(
    val query: String = "",
    val species: List<ReferenceModel> = emptyList(),
    val selectedCategories: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

sealed interface SharedGuideCommand {

    data class InputSearchQuery(
        val query: String
    ) : SharedGuideCommand

    data class CheckCategory(
        val check: String
    ) : SharedGuideCommand

    data class ToggleNotification(
        val id: Int,
        val enabled: Int
    ) : SharedGuideCommand
}
