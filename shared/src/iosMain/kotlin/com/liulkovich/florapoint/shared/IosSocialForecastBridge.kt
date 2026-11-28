
package com.liulkovich.florapoint.shared

import com.liulkovich.florapoint.shared.domain.SharedSocialForecastGateway
import com.liulkovich.florapoint.shared.domain.SharedSocialForecastListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal sealed interface IosSocialForecastResult {

    data class Success(
        val message: String,
        val species: List<String>,
        val postsAnalyzed: Int
    ) : IosSocialForecastResult

    data class Failure(
        val noInternet: Boolean
    ) : IosSocialForecastResult
}

internal suspend fun requestIosSocialForecast(
    gateway: SharedSocialForecastGateway,
    latitude: Double,
    longitude: Double,
    language: String
): IosSocialForecastResult =
    suspendCancellableCoroutine { continuation ->

        try {
            gateway.fetch(
                latitude = latitude,
                longitude = longitude,
                language = language,

                listener = object : SharedSocialForecastListener {

                    override fun onSuccess(
                        message: String,
                        species: List<String>,
                        postsAnalyzed: Int
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(
                                IosSocialForecastResult.Success(
                                    message = message,
                                    species = species,
                                    postsAnalyzed = postsAnalyzed
                                )
                            )
                        }
                    }

                    override fun onFailure(
                        noInternet: Boolean
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(
                                IosSocialForecastResult.Failure(
                                    noInternet = noInternet
                                )
                            )
                        }
                    }
                }
            )
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(
                    IosSocialForecastResult.Failure(
                        noInternet = false
                    )
                )
            }
        }
    }
