
package com.liulkovich.florapoint.shared.domain

interface SharedSocialForecastGateway {

    fun fetch(
        latitude: Double,
        longitude: Double,
        language: String,
        listener: SharedSocialForecastListener
    )
}

interface SharedSocialForecastListener {

    fun onSuccess(
        message: String,
        species: List<String>,
        postsAnalyzed: Int
    )

    fun onFailure(
        noInternet: Boolean
    )
}
