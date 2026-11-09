package com.liulkovich.florapoint.shared.domain

enum class SharedFloraCategory(
    val key: String,
    val imageName: String,
    val emoji: String
) {
    MUSHROOM("mushroom", "mushroom", "🍄"),
    BERRY("berry", "berry", "🫐"),
    PLANT("plant", "plant", "🌿"),
    NUT("nut", "nut", "🌰"),
    OTHER("other", "other", "📍");

    companion object {
        fun fromKey(key: String): SharedFloraCategory? =
            entries.find { it.key == key }
    }
}
