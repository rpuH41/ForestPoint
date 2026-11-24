
package com.liulkovich.florapoint.shared

import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liulkovich.florapoint.shared.domain.SharedUserPoint

private fun SQLiteStatement.textOrNull(
    index: Int
): String? =
    if (isNull(index)) null else getText(index)

private fun SQLiteStatement.intOrNull(
    index: Int
): Int? =
    if (isNull(index)) null else getInt(index)

private fun SQLiteStatement.longOrNull(
    index: Int
): Long? =
    if (isNull(index)) null else getLong(index)

private fun SQLiteStatement.doubleOrNull(
    index: Int
): Double? =
    if (isNull(index)) null else getDouble(index)

fun countIosUserPoints(): Int {
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->
        connection.prepare(
            "SELECT COUNT(*) FROM user_points_table"
        ).use { statement ->
            return if (statement.step()) {
                statement.getInt(0)
            } else {
                0
            }
        }
    }
}

fun loadIosUserPoints(): List<SharedUserPoint> {

    val points = mutableListOf<SharedUserPoint>()
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->

        connection.prepare(
            """
            SELECT
                id,
                species_id,
                latitude,
                longitude,
                user_name,
                description,
                timestamp,
                is_favorite,
                photo_path,
                accuracy,
                category,
                temperature,
                humidity,
                avg_temp_5days,
                avg_humidity_5days,
                weather_timestamp,
                is_public,
                owner_uid,
                cloud_id,
                sync_state
            FROM user_points_table
            ORDER BY timestamp DESC, id DESC
            """.trimIndent()
        ).use { statement ->

            while (statement.step()) {

                val point = SharedUserPoint(
                    id = statement.getInt(0),
                    speciesId = statement.intOrNull(1),

                    latitude = statement.getDouble(2),
                    longitude = statement.getDouble(3),

                    userName = statement.getText(4),
                    description = statement.getText(5),
                    timestamp = statement.getLong(6),

                    isFavorite = statement.getInt(7),
                    photoPath = statement.getText(8),
                    accuracy = statement.getInt(9),
                    category = statement.textOrNull(10),

                    temperature = statement.doubleOrNull(11),
                    humidity = statement.intOrNull(12),
                    avgTemp5Days = statement.doubleOrNull(13),
                    avgHumidity5Days = statement.intOrNull(14),
                    weatherTimestamp = statement.longOrNull(15),

                    isPublic = statement.getInt(16) != 0,
                    ownerUid = statement.textOrNull(17),
                    cloudId = statement.textOrNull(18),
                    syncState = statement.getText(19)
                )

                points.add(point)
            }
        }
    }

    return points
}
