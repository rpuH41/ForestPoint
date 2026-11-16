package com.liulkovich.florapoint.shared.domain

data class ReferenceModel(
    val id: Int,
    val category: String,

    val nameRu: String,
    val nameEn: String?,

    val habitatRu: String,
    val habitatEn: String?,

    val lookAlikesRu: String,
    val lookAlikesEn: String?,

    val descriptionRu: String,
    val descriptionEn: String?,

    val startMonth: Int,
    val endMonth: Int,

    val imageName: String,

    val isNotifEnabled: Int,
    val isReferenceOnly: Int = 0,

    val differencesRu: String? = null,
    val differencesEn: String? = null
)
