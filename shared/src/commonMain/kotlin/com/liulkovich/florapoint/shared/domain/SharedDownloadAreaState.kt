
package com.liulkovich.florapoint.shared.domain

data class SharedDownloadAreaProgress(
    val current: Int = 0,
    val total: Int = 0,
    val isFinished: Boolean = false,
    val isError: Boolean = false
)

data class SharedDownloadAreaEstimate(
    val areaKm2: Double = 0.0,
    val tilesCount: Int = 0,
    val approximateSizeMb: Double = 0.0
)

data class SharedDownloadAreaState(
    val isDownloading: Boolean = false,
    val progress: SharedDownloadAreaProgress? = null
)
