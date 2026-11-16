package com.liulkovich.florapoint

import com.liulkovich.florapoint.shared.SharedGreeting
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedModuleTest {

    @Test
    fun sharedGreetingIsAccessibleFromAndroid() {
        val message = SharedGreeting.getMessage()

        assertEquals(
            "ForestPoint Kotlin Multiplatform работает!",
            message
        )
    }
}
