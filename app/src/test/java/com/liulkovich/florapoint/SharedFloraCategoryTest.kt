
package com.liulkovich.florapoint

import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedFloraCategoryTest {

    @Test
    fun mushroomKeyReturnsMushroomCategory() {
        assertEquals(
            SharedFloraCategory.MUSHROOM,
            SharedFloraCategory.fromKey("mushroom")
        )
    }

    @Test
    fun allCategoriesHaveExpectedKeys() {
        assertEquals(
            listOf("mushroom", "berry", "plant", "nut", "other"),
            SharedFloraCategory.entries.map { it.key }
        )
    }

    @Test
    fun unknownCategoryReturnsNull() {
        assertNull(
            SharedFloraCategory.fromKey("unknown")
        )
    }
}
