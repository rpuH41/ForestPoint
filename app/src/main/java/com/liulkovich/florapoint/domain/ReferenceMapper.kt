package com.liulkovich.florapoint.domain

import com.liulkovich.florapoint.shared.domain.ReferenceModel

fun Reference.toReferenceModel(): ReferenceModel {
    return ReferenceModel(
        id = id,
        category = category,

        nameRu = nameRu,
        nameEn = nameEn,

        habitatRu = habitatRu,
        habitatEn = habitatEn,

        lookAlikesRu = lookAlikesRu,
        lookAlikesEn = lookAlikesEn,

        descriptionRu = descriptionRu,
        descriptionEn = descriptionEn,

        startMonth = startMonth,
        endMonth = endMonth,

        imageName = imageName,

        isNotifEnabled = isNotifEnabled,
        isReferenceOnly = isReferenceOnly,

        differencesRu = differencesRu,
        differencesEn = differencesEn
    )
}
