package com.liulkovich.florapoint.shared

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liulkovich.florapoint.shared.domain.SharedUserPoint

/** Changes only local points: a cloud-linked point must be synchronized via Firebase. */
fun updateIosUserPoint(point: SharedUserPoint): Boolean {
    require(point.id > 0)
    require(point.userName.isNotBlank())

    BundledSQLiteDriver().open(floraDatabasePath()).use { connection ->
        connection.prepare(
            """
            UPDATE user_points_table
            SET species_id = ?, user_name = ?, description = ?, category = ?
            WHERE id = ? AND cloud_id IS NULL
            """.trimIndent()
        ).use { statement ->
            if (point.speciesId == null) statement.bindNull(1)
            else statement.bindLong(1, point.speciesId.toLong())
            statement.bindText(2, point.userName.trim())
            statement.bindText(3, point.description.trim())
            if (point.category == null) statement.bindNull(4)
            else statement.bindText(4, point.category)
            statement.bindLong(5, point.id.toLong())
            statement.step()
        }

        connection.prepare("SELECT changes()").use { statement ->
            check(statement.step())
            return statement.getLong(0) > 0L
        }
    }
}

/** A cloud-linked point must first be deleted through its Firestore sync workflow. */
fun deleteIosUserPoint(id: Int): Boolean {
    require(id > 0)

    BundledSQLiteDriver().open(floraDatabasePath()).use { connection ->
        connection.prepare(
            "DELETE FROM user_points_table WHERE id = ? AND cloud_id IS NULL"
        ).use { statement ->
            statement.bindLong(1, id.toLong())
            statement.step()
        }

        connection.prepare("SELECT changes()").use { statement ->
            check(statement.step())
            return statement.getLong(0) > 0L
        }
    }
}
