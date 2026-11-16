package com.liulkovich.florapoint.shared.domain

private fun localizedText(
    russian: String,
    english: String?,
    language: String
): String {
    return if (language == "en") {
        english?.takeIf { it.isNotBlank() } ?: russian
    } else {
        russian
    }
}

fun ReferenceModel.localizedName(language: String): String =
    localizedText(nameRu, nameEn, language)

fun ReferenceModel.localizedHabitat(language: String): String =
    localizedText(habitatRu, habitatEn, language)

fun ReferenceModel.localizedLookAlikes(language: String): String =
    localizedText(lookAlikesRu, lookAlikesEn, language)

fun ReferenceModel.localizedDescription(language: String): String =
    localizedText(descriptionRu, descriptionEn, language)

fun ReferenceModel.localizedDifferences(language: String): String =
    localizedText(
        differencesRu ?: "",
        differencesEn,
        language
    )
