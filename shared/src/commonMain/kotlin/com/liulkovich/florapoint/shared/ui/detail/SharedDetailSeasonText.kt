
package com.liulkovich.florapoint.shared.ui.detail

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun sharedDetailSeasonText(
    startMonth: Int,
    endMonth: Int
): String {
    if (startMonth !in 1..12 || endMonth !in 1..12) {
        return stringResource(
            Res.string.detail_season_not_specified
        )
    }

    val startMonths = listOf(
        Res.string.from_january,
        Res.string.from_february,
        Res.string.from_march,
        Res.string.from_april,
        Res.string.from_may,
        Res.string.from_june,
        Res.string.from_july,
        Res.string.from_august,
        Res.string.from_september,
        Res.string.from_october,
        Res.string.from_november,
        Res.string.from_december
    )

    val endMonths = listOf(
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
        startMonths[startMonth - 1]
    )

    val endName = stringResource(
        endMonths[endMonth - 1]
    )

    return stringResource(
        Res.string.from_to,
        startName,
        endName
    )
}
