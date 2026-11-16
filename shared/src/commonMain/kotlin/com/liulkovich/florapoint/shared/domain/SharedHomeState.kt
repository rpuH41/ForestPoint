package com.liulkovich.florapoint.shared.domain

data class SharedHomeScreenState(
    val species: List<ReferenceModel> = emptyList(),
    val tipText: String? = null,
    val isLoading: Boolean = true,
    val isTipLoading: Boolean = true,
    val selectedSeasonCategory: String? = null
)
