package com.liulkovich.florapoint.shared.domain

enum class ForestType {
    CONIFEROUS,
    DECIDUOUS,
    MIXED
}

fun ReferenceModel.matchesForest(
    forest: ForestType?
): Boolean {
    if (forest == null) return true

    if (category != SharedFloraCategory.MUSHROOM.key) {
        return false
    }

    val habitat = habitatRu.lowercase()

    return when (forest) {
        ForestType.CONIFEROUS -> listOf(
            "хвойн", "соснов", "елов", "ельник"
        ).any { it in habitat }

        ForestType.DECIDUOUS -> listOf(
            "лиственн", "берёз", "берез",
            "осинов", "осинник", "дубов", "буков"
        ).any { it in habitat }

        ForestType.MIXED -> "смешанн" in habitat
    }
}
