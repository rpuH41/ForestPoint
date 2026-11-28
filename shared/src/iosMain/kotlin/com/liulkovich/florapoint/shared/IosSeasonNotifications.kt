package com.liulkovich.florapoint.shared

import com.liulkovich.florapoint.shared.domain.SharedNotificationSpecies
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private const val SEASON_PREFIX = "forestpoint.season."
private const val TEST_ID = "forestpoint.season-test"

// Keep a safety margin for other local notifications and the test.
// Requests for several species on one date are combined before this cap.
private const val MAX_SEASON_REQUESTS = 60
private const val REMINDER_HOUR = 10

private enum class SeasonStage { START, PEAK, END }

private data class CalendarDay(val year: Int, val month: Int, val day: Int)
private data class SpeciesEvent(
    val speciesName: String,
    val date: CalendarDay,
    val stage: SeasonStage
)
private data class ScheduledEvent(
    val identifier: String,
    val date: CalendarDay,
    val title: String,
    val body: String
)

private fun allowed(status: UNAuthorizationStatus?): Boolean =
    status == UNAuthorizationStatusAuthorized ||
        status == UNAuthorizationStatusProvisional

/** Ask only from a deliberate button tap in Settings. */
fun requestIosSeasonNotificationPermission(onResult: (Boolean) -> Unit) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound
    ) { granted, error ->
        onResult(granted && error == null)
    }
}

private fun leap(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (leap(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun nextDay(value: CalendarDay): CalendarDay = when {
    value.day < daysInMonth(value.year, value.month) ->
        value.copy(day = value.day + 1)
    value.month < 12 -> CalendarDay(value.year, value.month + 1, 1)
    else -> CalendarDay(value.year + 1, 1, 1)
}

private fun midpoint(start: CalendarDay, end: CalendarDay): CalendarDay {
    // The calendar midpoint, not the 1st of a 'middle month'.
    var length = 0
    var cursor = start
    while (cursor != end && length < 370) {
        cursor = nextDay(cursor)
        length++
    }
    cursor = start
    repeat(length / 2) { cursor = nextDay(cursor) }
    return cursor
}

private fun order(value: CalendarDay): Int =
    value.year * 10000 + value.month * 100 + value.day

private fun nativeComponents(date: CalendarDay): NSDateComponents {
    val components = NSDateComponents()
    components.year = date.year.toLong()
    components.month = date.month.toLong()
    components.day = date.day.toLong()
    components.hour = REMINDER_HOUR.toLong()
    components.minute = 0L
    return components
}

private fun futureEvents(
    species: List<SharedNotificationSpecies>,
    preferences: IosNotificationPreferences
): List<SpeciesEvent> {
    val calendar = NSCalendar.currentCalendar
    val now = NSDate()
    val year = calendar.component(NSCalendarUnitYear, fromDate = now).toInt()
    val today = CalendarDay(
        year,
        calendar.component(NSCalendarUnitMonth, fromDate = now).toInt(),
        calendar.component(NSCalendarUnitDay, fromDate = now).toInt()
    )
    val hour = calendar.component(NSCalendarUnitHour, fromDate = now).toInt()
    val minute = calendar.component(NSCalendarUnitMinute, fromDate = now).toInt()

    fun isFuture(date: CalendarDay): Boolean =
        order(date) > order(today) ||
            (date == today && (hour * 60 + minute) < REMINDER_HOUR * 60)

    val result = mutableListOf<SpeciesEvent>()
    for (item in species) {
        if (item.startMonth !in 1..12 || item.endMonth !in 1..12) continue
        val events = mutableListOf<SpeciesEvent>()
        // Include the preceding year for seasons that cross New Year.
        for (seasonYear in (year - 1)..(year + 2)) {
            val seasonEndYear = if (item.endMonth < item.startMonth) {
                seasonYear + 1
            } else {
                seasonYear
            }
            val start = CalendarDay(seasonYear, item.startMonth, 1)
            val end = CalendarDay(
                seasonEndYear,
                item.endMonth,
                daysInMonth(seasonEndYear, item.endMonth)
            )
            if (preferences.notifyStart) {
                events += SpeciesEvent(item.displayName, start, SeasonStage.START)
            }
            if (preferences.notifyPeak) {
                events += SpeciesEvent(item.displayName, midpoint(start, end), SeasonStage.PEAK)
            }
            if (preferences.notifyEnd) {
                events += SpeciesEvent(item.displayName, end, SeasonStage.END)
            }
        }
        // Schedule the nearest future occurrence for each stage of this species.
        result += events.filter { isFuture(it.date) }
            .groupBy { it.stage }
            .values
            .mapNotNull { occurrences -> occurrences.minByOrNull { order(it.date) } }
    }
    return result
}

private fun groupedRequests(
    species: List<SharedNotificationSpecies>,
    preferences: IosNotificationPreferences,
    language: String
): List<ScheduledEvent> {
    val ru = language == "ru"
    val groups = futureEvents(species, preferences)
        .groupBy { it.date to it.stage }
        .values

    val requests = groups.map { events ->
        val first = events.first()
        val stage = first.stage
        val typeName = when (stage) {
            SeasonStage.START -> if (ru) "Начало сезона" else "Season begins"
            SeasonStage.PEAK -> if (ru) "Пик сезона" else "Peak season"
            SeasonStage.END -> if (ru) "Конец сезона" else "Season ends"
        }
        val title = if (events.size == 1) {
            "$typeName: ${first.speciesName}"
        } else if (ru) {
            "$typeName: ${events.size} видов"
        } else {
            "$typeName: ${events.size} species"
        }
        val displayed = events.take(4).joinToString(", ") { it.speciesName }
        val extra = events.size - 4
        val body = if (extra > 0) {
            if (ru) "$displayed и ещё $extra" else "$displayed and $extra more"
        } else {
            displayed
        }
        val date = first.date
        ScheduledEvent(
            identifier = "$SEASON_PREFIX${date.year}.${date.month}.${date.day}.${stage.name}",
            date = date,
            title = title,
            body = body
        )
    }

    val ordered = requests.sortedWith(
        compareBy<ScheduledEvent> { order(it.date) }.thenBy { it.identifier }
    )
    if (ordered.size > MAX_SEASON_REQUESTS) {
        println("ForestPoint: ${ordered.size} groups; scheduling nearest $MAX_SEASON_REQUESTS")
    }
    return ordered.take(MAX_SEASON_REQUESTS)
}

/** Schedule upcoming one-off events with true calendar dates (incl. leap years). */
fun synchronizeIosSeasonNotifications(
    species: List<SharedNotificationSpecies>,
    preferences: IosNotificationPreferences,
    language: String
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val future = groupedRequests(species, preferences, language)

    center.getPendingNotificationRequestsWithCompletionHandler { pending ->
        // Kotlin/Native nullable Objective-C collection: cast request explicitly.
        val previousIds: List<String> = pending
            ?.mapNotNull { candidate ->
                (candidate as? UNNotificationRequest)?.identifier
                    ?.takeIf { id -> id.startsWith(SEASON_PREFIX) }
            }
            .orEmpty()
        if (previousIds.isNotEmpty()) {
            center.removePendingNotificationRequestsWithIdentifiers(previousIds)
        }

        if (future.isEmpty()) return@getPendingNotificationRequestsWithCompletionHandler

        center.getNotificationSettingsWithCompletionHandler { settings ->
            if (!allowed(settings?.authorizationStatus)) {
                println("ForestPoint notifications: permission not granted")
                return@getNotificationSettingsWithCompletionHandler
            }
            for (reminder in future) {
                val content = UNMutableNotificationContent()
                content.setTitle(reminder.title)
                content.setBody(reminder.body)
                content.setSound(UNNotificationSound.defaultSound())

                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    nativeComponents(reminder.date),
                    repeats = false
                )
                val request = UNNotificationRequest.requestWithIdentifier(
                    reminder.identifier,
                    content,
                    trigger
                )
                center.addNotificationRequest(request, withCompletionHandler = { error ->
                    if (error != null) {
                        println("ForestPoint notification ${reminder.identifier}: ${error.localizedDescription}")
                    }
                })
            }
            println("ForestPoint: scheduled ${future.size} seasonal dates")
        }
    }
}

/** Diagnostic only; background the app and wait for its local test alert. */
fun scheduleIosSeasonNotificationTest(language: String, onResult: (Boolean) -> Unit) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.getNotificationSettingsWithCompletionHandler { settings ->
        if (!allowed(settings?.authorizationStatus)) {
            onResult(false)
            return@getNotificationSettingsWithCompletionHandler
        }
        val content = UNMutableNotificationContent()
        content.setTitle("ForestPoint")
        content.setBody(
            if (language == "ru") {
                "Проверка уведомлений: всё работает!"
            } else {
                "Notification test: it works!"
            }
        )
        content.setSound(UNNotificationSound.defaultSound())
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            15.0,
            repeats = false
        )
        val request = UNNotificationRequest.requestWithIdentifier(TEST_ID, content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = { error ->
            onResult(error == null)
        })
    }
}
