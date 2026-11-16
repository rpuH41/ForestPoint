
package com.liulkovich.florapoint

import com.liulkovich.florapoint.shared.domain.ForestType
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import com.liulkovich.florapoint.shared.domain.matchesForest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideForestFilterTest {

    private fun createReference(
        category: String = "mushroom",
        habitat: String = ""
    ) = ReferenceModel(
        id = 1,
        category = category,
        nameRu = "Тестовый вид",
        nameEn = null,
        habitatRu = habitat,
        habitatEn = null,
        lookAlikesRu = "",
        lookAlikesEn = null,
        descriptionRu = "",
        descriptionEn = null,
        startMonth = 6,
        endMonth = 10,
        imageName = "test",
        isNotifEnabled = 0
    )

    @Test
    fun noForestFilterAcceptsAnySpecies() {
        val berry = createReference(
            category = "berry",
            habitat = "Лес"
        )

        assertTrue(berry.matchesForest(null))
    }

    @Test
    fun coniferousForestMatchesPineHabitat() {
        val mushroom = createReference(
            habitat = "Растёт в сосновых лесах"
        )

        assertTrue(
            mushroom.matchesForest(ForestType.CONIFEROUS)
        )
    }

    @Test
    fun deciduousForestMatchesBirchHabitat() {
        val mushroom = createReference(
            habitat = "Растёт в берёзовых рощах"
        )

        assertTrue(
            mushroom.matchesForest(ForestType.DECIDUOUS)
        )
    }

    @Test
    fun mixedForestMatchesMixedHabitat() {
        val mushroom = createReference(
            habitat = "Встречается в смешанных лесах"
        )

        assertTrue(
            mushroom.matchesForest(ForestType.MIXED)
        )
    }

    @Test
    fun nonMushroomIsExcludedWhenForestSelected() {
        val berry = createReference(
            category = "berry",
            habitat = "Сосновый лес"
        )

        assertFalse(
            berry.matchesForest(ForestType.CONIFEROUS)
        )
    }

    @Test
    fun unrelatedHabitatDoesNotMatch() {
        val mushroom = createReference(
            habitat = "Растёт на открытых лугах"
        )

        assertFalse(
            mushroom.matchesForest(ForestType.CONIFEROUS)
        )
    }
}
