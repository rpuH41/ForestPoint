package com.liulkovich.florapoint.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.liulkovich.florapoint.domain.SocialForecast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class NoInternetException(
    cause: Throwable? = null
) : Exception("No internet connection", cause)

class SocialForecastServiceException(
    cause: Throwable? = null
) : Exception("Social forecast service unavailable", cause)

@Singleton
class SocialForecastRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val functions: FirebaseFunctions =
        Firebase.functions("us-central1")

    suspend fun getSocialForecast(
        lat: Double,
        lon: Double,
        language: String
    ): Result<SocialForecast> {
        return try {
            val data = hashMapOf(
                "lat" to lat,
                "lon" to lon,
                "language" to language
            )

            val result = functions
                .getHttpsCallable("getForecast")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val map =
                result.data as? Map<String, Any>
                    ?: emptyMap()

            val message =
                map["message"] as? String ?: ""

            val species =
                (map["species"] as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

            val posts =
                (map["postsAnalyzed"] as? Number)
                    ?.toInt()
                    ?: 0

            Result.success(
                SocialForecast(
                    message = message,
                    species = species,
                    postsAnalyzed = posts
                )
            )
        } catch (e: Exception) {

            if (!hasInternetConnection()) {
                Result.failure(
                    NoInternetException(e)
                )
            } else {
                Result.failure(
                    SocialForecastServiceException(e)
                )
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager
                .getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
    }
}