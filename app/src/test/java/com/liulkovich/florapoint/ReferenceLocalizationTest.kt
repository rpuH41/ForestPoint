
package com.liulkovich.florapoint

import com.liulkovich.florapoint.shared.domain.ReferenceModel
import com.liulkovich.florapoint.shared.domain.localizedName
import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceLocalizationTest {

    private val reference = ReferenceModel(
        id = 1,
        category = "mushroom",
        nameRu = "Белый гриб",
        nameEn = "Porcini",
        habitatRu = "Хвойные леса",
        habitatEn = "Coniferous forests",
        lookAlikesRu = "",
        lookAlikesEn = null,
        descriptionRu = "Съедобный гриб",
        descriptionEn = "Edible mushroom",
        startMonth = 6,
        endMonth = 10,
        imageName = "mushroom",
        isNotifEnabled = 1
    )

    @Test
    fun russianNameIsReturned() {
        assertEquals(
            "Белый гриб",
            reference.localizedName("ru")
        )
    }

    @Test
    fun englishNameIsReturned() {
        assertEquals(
            "Porcini",
            reference.localizedName("en")
        )
    }

    @Test
    fun russianNameIsUsedWhenEnglishTranslationIsMissing() {
        val withoutEnglish = reference.copy(nameEn = null)

        assertEquals(
            "Белый гриб",
            withoutEnglish.localizedName("en")
        )
    }

    @Test
    fun russianNameIsUsedWhenEnglishTranslationIsBlank() {
        val withoutEnglish = reference.copy(nameEn = " ")

        assertEquals(
            "Белый гриб",
            withoutEnglish.localizedName("en")
        )
    }
}
