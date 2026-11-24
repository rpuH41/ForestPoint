package com.liulkovich.florapoint.shared

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liulkovich.florapoint.shared.domain.SharedUserPoint

/** Writes a new private, local point using the same flora.db as the species guide. */
fun insertIosUserPoint(point: SharedUserPoint): Long {
    require(point.userName.isNotBlank())
    require(point.latitude in -90.0..90.0)
    require(point.longitude in -180.0..180.0)

    BundledSQLiteDriver().open(floraDatabasePath()).use { connection ->
        connection.prepare(
            """
            INSERT INTO user_points_table (
                species_id, latitude, longitude, user_name, description,
                timestamp, is_favorite, photo_path, accuracy, category,
                is_public, owner_uid, cloud_id, sync_state
            ) VALUES (?, ?, ?, ?, ?, ?, 0, '', 0, ?, 0, NULL, NULL, 'LOCAL')
            """.trimIndent()
        ).use { statement ->
            if (point.speciesId == null) statement.bindNull(1)
            else statement.bindLong(1, point.speciesId.toLong())
            statement.bindDouble(2, point.latitude)
            statement.bindDouble(3, point.longitude)
            statement.bindText(4, point.userName.trim())
            statement.bindText(5, point.description.trim())
            statement.bindLong(6, point.timestamp)
            if (point.category == null) statement.bindNull(7)
            else statement.bindText(7, point.category)
            statement.step()
        }

        connection.prepare("SELECT last_insert_rowid()").use { statement ->
            check(statement.step())
            return statement.getLong(0)
        }
    }
}
