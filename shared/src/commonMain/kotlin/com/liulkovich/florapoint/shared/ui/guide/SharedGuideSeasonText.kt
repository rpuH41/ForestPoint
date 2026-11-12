
package com.liulkovich.florapoint.shared.ui.guide

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun sharedGuideSeasonText(
    startMonth: Int,
    endMonth: Int
): String {
    if (startMonth !in 1..12 || endMonth !in 1..12) {
        return ""
    }

    val months = listOf(
        Res.string.january,
        Res.string.february,
        Res.string.march,
        Res.string.april,
        Res.string.may,
        Res.string.june,
        Res.string.july,
        Res.string.august,
        Res.string.september,
        Res.string.october,
        Res.string.november,
        Res.string.december
    )

    val startName = stringResource(
        months[startMonth - 1]
    )

    val endName = stringResource(
        months[endMonth - 1]
    )

    return stringResource(
        Res.string.season,
        startName,
        endName
    )
}
