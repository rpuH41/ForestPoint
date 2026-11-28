
package com.liulkovich.florapoint.shared.domain

private val dangerousMushroomIds = setOf(
    26, 27, 75, 74, 73, 79, 72
)

fun ReferenceModel.isDangerousMushroom(): Boolean =
    id in dangerousMushroomIds
