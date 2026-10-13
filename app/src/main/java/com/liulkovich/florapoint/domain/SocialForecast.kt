package com.liulkovich.florapoint.domain

data class SocialForecast(
    val message: String,
    val species: List<String> = emptyList(),
    val postsAnalyzed: Int = 0
)
