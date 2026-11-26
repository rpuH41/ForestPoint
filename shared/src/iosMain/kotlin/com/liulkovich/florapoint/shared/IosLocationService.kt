
package com.liulkovich.florapoint.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

import kotlin.coroutines.resume

data class IosCoordinates(
    val latitude: Double,
    val longitude: Double
)

@OptIn(ExperimentalForeignApi::class)
private class IosLocationRequest(
    private val continuation: CancellableContinuation<IosCoordinates?>
) : NSObject(), CLLocationManagerDelegateProtocol {

    private val manager = CLLocationManager()

    private var permissionRequested = false
    private var locationRequested = false
    private var finished = false

    init {
        manager.delegate = this
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    fun start() {
        checkAuthorization()
    }

    private fun checkAuthorization() {
        if (finished) return

        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                if (!locationRequested) {
                    locationRequested = true
                    manager.requestLocation()
                }
            }

            kCLAuthorizationStatusNotDetermined -> {
                if (!permissionRequested) {
                    permissionRequested = true
                    manager.requestWhenInUseAuthorization()
                }
            }

            else -> finish(null)
        }
    }

    override fun locationManagerDidChangeAuthorization(
        manager: CLLocationManager
    ) {
        checkAuthorization()
    }

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val location = didUpdateLocations
            .lastOrNull() as? CLLocation

        val coordinates = location?.coordinate?.useContents {
            IosCoordinates(
                latitude = latitude,
                longitude = longitude
            )
        }

        finish(coordinates)
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        println("CoreLocation: ${didFailWithError.localizedDescription}")
        finish(null)
    }

    fun cancel() {
        if (finished) return

        finished = true
        manager.stopUpdatingLocation()
        manager.delegate = null
    }

    private fun finish(coordinates: IosCoordinates?) {
        if (finished) return

        cancel()

        if (continuation.isActive) {
            continuation.resume(coordinates)
        }
    }
}

suspend fun requestIosCoordinates(): IosCoordinates? =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val request = IosLocationRequest(continuation)

            continuation.invokeOnCancellation {
                request.cancel()
            }

            request.start()
        }
    }
